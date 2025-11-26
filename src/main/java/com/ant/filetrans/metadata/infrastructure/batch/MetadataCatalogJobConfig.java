package com.ant.filetrans.metadata.infrastructure.batch;

import com.ant.filetrans.metadata.application.MetadataPersistenceService;
import com.ant.filetrans.metadata.domain.FileMetadata;
import com.ant.filetrans.metadata.domain.MetadataCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetadataCatalogJobConfig {

    private final ObjectMapper objectMapper;
    private final MetadataPersistenceService persistenceService;

    @Bean
    public Job metadataCatalogJob(JobRepository jobRepository,
                                  Step metadataCatalogStep) {
        return new JobBuilder("metadataCatalogJob", jobRepository)
                .start(metadataCatalogStep)
                .build();
    }

    @Bean
    public Step metadataCatalogStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    ItemStreamReader<Path> metadataSidecarReader,
                                    ItemProcessor<Path, MetadataCatalog.CatalogEntry> catalogEntryProcessor,
                                    MetadataCatalogWriter metadataCatalogWriter) {

        return new StepBuilder("metadataCatalogStep", jobRepository)
                .<Path, MetadataCatalog.CatalogEntry>chunk(100)
                .reader(metadataSidecarReader)
                .processor(catalogEntryProcessor)
                .writer(metadataCatalogWriter)
                .transactionManager(transactionManager)
                .listener(metadataCatalogWriter)
                .build();
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public MetadataSidecarReader metadataSidecarReader(@Value("#{jobParameters['targetBaseDir']}") String targetBaseDir) {
        return new MetadataSidecarReader(Path.of(targetBaseDir));
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public ItemProcessor<Path, MetadataCatalog.CatalogEntry> catalogEntryProcessor(
            @Value("#{jobParameters['targetBaseDir']}") String targetBaseDir) {
        Path root = Path.of(targetBaseDir);
        return jsonPath -> {
            FileMetadata metadata = objectMapper.readValue(jsonPath.toFile(), FileMetadata.class);
            Path relative = root.relativize(metadata.file());
            return new MetadataCatalog.CatalogEntry(
                    relative.toString(),
                    metadata.size(),
                    metadata.contentType(),
                    metadata.lastModified()
            );
        };
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public MetadataCatalogWriter metadataCatalogWriter(@Value("#{jobParameters['targetBaseDir']}") String targetBaseDir) {
        MetadataCatalogWriter writer = new MetadataCatalogWriter(persistenceService);
        writer.setTargetBaseDir(targetBaseDir);
        return writer;
    }
}
