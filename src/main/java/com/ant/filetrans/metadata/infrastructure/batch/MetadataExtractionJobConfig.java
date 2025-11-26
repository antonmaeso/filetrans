package com.ant.filetrans.metadata.infrastructure.batch;

import com.ant.filetrans.metadata.application.FileMetadataService;
import com.ant.filetrans.metadata.application.MetadataPersistenceService;
import com.ant.filetrans.metadata.domain.MetadataProcessingResult;
import com.ant.filetrans.metadata.infrastructure.persistence.MetadataWorkItemEntity;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetadataExtractionJobConfig {

    @Bean
    public Job metadataExtractionJob(JobRepository jobRepository,
                                     Step metadataExtractionStep,
                                     JobExecutionListener jobExecutionLogger) {
        return new JobBuilder("metadataExtractionJob", jobRepository)
                .listener(jobExecutionLogger)
                .start(metadataExtractionStep)
                .build();
    }

    @Bean
    public Step metadataExtractionStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       JpaPagingItemReader<MetadataWorkItemEntity> metadataWorkReader,
                                       ItemProcessor<MetadataWorkItemEntity, MetadataProcessingResult> metadataProcessor,
                                       ItemWriter<MetadataProcessingResult> metadataWriter,
                                       StepExecutionListener stepExecutionLogger) {

        return new StepBuilder("metadataExtractionStep", jobRepository)
                .listener(stepExecutionLogger)
                .<MetadataWorkItemEntity, MetadataProcessingResult>chunk(50)
                .reader(metadataWorkReader)
                .processor(metadataProcessor)
                .writer(metadataWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public JpaPagingItemReader<MetadataWorkItemEntity> metadataWorkReader(EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<MetadataWorkItemEntity>()
                .name("metadataWorkReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT w FROM MetadataWorkItemEntity w WHERE w.status = 'PENDING' ORDER BY w.id")
                .pageSize(50)
                .build();
    }

    @Bean
    public ItemProcessor<MetadataWorkItemEntity, MetadataProcessingResult> metadataProcessor(FileMetadataService metadataService) {
        return entity -> {
            var metadata = metadataService.capture(Path.of(entity.getFilePath()));
            return new MetadataProcessingResult(entity.getId(), metadata);
        };
    }

    @Bean
    public ItemWriter<MetadataProcessingResult> metadataWriter(MetadataPersistenceService persistenceService) {
        return chunk -> chunk.forEach(persistenceService::writeMetadata);
    }
}
