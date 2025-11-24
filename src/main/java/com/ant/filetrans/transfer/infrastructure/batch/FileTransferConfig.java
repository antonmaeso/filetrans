package com.ant.filetrans.transfer.infrastructure.batch;

import com.ant.filetrans.transfer.domain.FileDescriptor;
import com.ant.filetrans.transfer.domain.MovedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
public class FileTransferConfig {

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter DAY  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Job fileImportJob(JobRepository jobRepository,
                             Step fileImportStep,
                             JobExecutionListener jobExecutionLogger) {

        return new JobBuilder("photoImportJob", jobRepository)
                .listener(jobExecutionLogger)
                .start(fileImportStep)
                .build();
    }

    @Bean
    public Step fileImportStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               ItemReader<FileDescriptor> photoFileReader,
                               ItemProcessor<FileDescriptor, MovedFile> photoFileProcessor,
                               ItemWriter<MovedFile> fileWriter,
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
    public ItemReader<FileDescriptor> photoFileReader(
            @org.springframework.beans.factory.annotation.Value("#{jobParameters['sourceDir']}") String sourceDir,
            @org.springframework.beans.factory.annotation.Value("#{jobParameters['extensions']}") String extensionsParam) {

        log.info("Scanning files under {} with extensions {}", sourceDir, extensionsParam);
        return new FileItemReader(sourceDir, parseExtensions(extensionsParam));
    }

    @Bean
    @StepScope
    public ItemProcessor<FileDescriptor, MovedFile> photoFileProcessor(
            @org.springframework.beans.factory.annotation.Value("#{jobParameters['targetBaseDir']}") String targetBaseDir) {

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
    public ItemWriter<MovedFile> fileWriter() {
        return chunk -> {
            var items = chunk.getItems();
            log.info("Moving {} files in current chunk", items.size());
            for (MovedFile mp : items) {
                Path source = mp.source();
                Path target = mp.target();

                Files.createDirectories(target.getParent());
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                log.info("Moved {} -> {}", source, target);
            }
        };
    }

    private static java.util.Set<String> parseExtensions(String extensionsParam) {
        if (extensionsParam == null || extensionsParam.isBlank()) {
            return java.util.Set.of();
        }
        return java.util.Arrays.stream(extensionsParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
