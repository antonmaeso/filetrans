package com.ant.filetrans.transfer.infrastructure.batch;

import com.ant.filetrans.transfer.domain.FileDescriptor;
import com.ant.filetrans.transfer.domain.MovedFile;
import com.ant.filetrans.transfer.application.FileMoveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@EnableBatchProcessing
@EnableJdbcJobRepository
@Configuration
@RequiredArgsConstructor
public class FileTransferConfig {

    public static final String BASE_DIR_PARAM = "targetBaseDir";

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter DAY  = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Bean
    public Job photoImportJob(JobRepository jobRepository,
                             Step transferStep,
                             JobExecutionListener jobExecutionLogger,
                             TransferJobCompletionPublisher completionPublisher) {

        return new JobBuilder("photoImportJob", jobRepository)
                .listener(jobExecutionLogger)
                .listener(completionPublisher)
                .start(transferStep)
                .build();
    }

    @Bean
    public Step transferStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             ItemStreamReader<FileDescriptor> photoFileReader,
                             ItemProcessor<FileDescriptor, MovedFile> photoFileProcessor,
                             FileMoveItemWriter fileWriter,
                             StepExecutionListener stepExecutionLogger) {

        return new StepBuilder("FileTransferStep", jobRepository)
                .listener(stepExecutionLogger)
                .<FileDescriptor, MovedFile>chunk(50)
                .reader(photoFileReader)
                .processor(photoFileProcessor)
                .writer(fileWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public JobExecutionListener jobExecutionLogger() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("Job {} starting with parameters {}", jobExecution.getJobInstance().getJobName(), jobExecution.getJobParameters());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                log.info("Job {} completed with status {}", jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());
            }
        };
    }

    @Bean
    public StepExecutionListener stepExecutionLogger() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("Step {} starting", stepExecution.getStepName());
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("Step {} finished, read={}, written={}, skipped={}",
                        stepExecution.getStepName(),
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getSkipCount());
                return stepExecution.getExitStatus();
            }
        };
    }

    @Bean
    @StepScope
    public ItemStreamReader<FileDescriptor> photoFileReader(
            @Value("#{jobParameters['sourceDir']}") String sourceDir,
            @Value("#{jobParameters['extensions']}") String extensionsParam) {

        Extensions extensions = Extensions.parse(extensionsParam);
        log.info("Creating reader for {} with extensions {}", sourceDir, extensions.values());
        return new FileItemReader(sourceDir, extensions);
    }

    @Bean
    @StepScope
    public ItemProcessor<FileDescriptor, MovedFile> photoFileProcessor(
            @Value("#{jobParameters['targetBaseDir']}") String targetBaseDir) {

        return file -> {
            var zdt  = file.lastModified().atZone(ZoneId.systemDefault());
            var year = YEAR.format(zdt);
            var day  = DAY.format(zdt);

            Path targetDir = Path.of(targetBaseDir, year, day);
            Path target    = targetDir.resolve(file.path().getFileName());

            log.debug("Prepared move {} -> {}", file.path(), target);
            return new MovedFile(file.path(), target);
        };
    }

    @Bean
    @StepScope
    public FileMoveItemWriter fileWriter(FileMoveService fileMoveService,
                                         @Value("#{jobParameters['targetBaseDir']}") String targetBaseDir,
                                         com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        DuplicateCatalogChecker duplicateCatalogChecker = new DuplicateCatalogChecker(Path.of(targetBaseDir), objectMapper);
        return new FileMoveItemWriter(fileMoveService, duplicateCatalogChecker);
    }

}
