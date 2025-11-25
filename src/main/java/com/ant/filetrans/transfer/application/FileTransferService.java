package com.ant.filetrans.transfer.application;

import com.ant.filetrans.transfer.infrastructure.batch.Extensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferService {

    public static final String SOURCE_DIR = "sourceDir";
    public static final String BASE_DIR = "targetBaseDir";
    public static final String TIMESTAMP = "timestamp";
    public static final String FILE_PATH = "filePath";

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final Job photoImportJob;

    public JobExecution transferDirectory(Path sourceDir, Path targetBaseDir, Extensions extensions) throws Exception {
        log.info("Starting directory transfer job from {} to {} with extensions {}", sourceDir, targetBaseDir, extensions);

        JobParametersBuilder builder = new JobParametersBuilder()
                .addString(SOURCE_DIR, sourceDir.toString())
                .addString(BASE_DIR, targetBaseDir.toString())
                .addLong(TIMESTAMP, System.currentTimeMillis());
        addExtensions(builder, extensions);
        JobParameters params = builder.toJobParameters();

        JobExecution execution = jobOperator.start(photoImportJob, params);
        log.info("Directory transfer job {} started", execution.getId());
        return execution;
    }

    public JobExecution transferSingleFile(Path file, Path targetBaseDir, Extensions extensions) throws Exception {
        log.info("Starting single file transfer for {} -> {} with extensions {}", file, targetBaseDir, extensions);

        JobParametersBuilder builder = new JobParametersBuilder()
                .addString(SOURCE_DIR, file.getParent().toString())
                .addString(FILE_PATH, file.toString())
                .addString(BASE_DIR, targetBaseDir.toString())
                .addLong(TIMESTAMP, System.currentTimeMillis());
        addExtensions(builder, extensions);
        JobParameters params = builder.toJobParameters();

        JobExecution execution = jobOperator.start(photoImportJob, params);
        log.info("Single file transfer job {} started", execution.getId());
        return execution;
    }

    public JobExecution getJobExecution(long executionId) {
        JobExecution execution = jobRepository.getJobExecution(executionId);
        if (execution == null) {
            log.warn("Requested job execution {} not found", executionId);
        } else {
            log.debug("Fetched job execution {} with status {}", executionId, execution.getStatus());
        }
        return execution;
    }

    public void stopJob(long executionId) throws JobExecutionNotRunningException {
        JobExecution execution = jobRepository.getJobExecution(executionId);
        if (execution == null) {
            log.warn("Attempted to stop non-existent job execution {}", executionId);
            throw new IllegalArgumentException("No JobExecution with id " + executionId);
        }

        log.info("Stopping job execution {}", executionId);
        jobOperator.stop(execution);
    }

    private static JobParametersBuilder addExtensions(JobParametersBuilder builder, Extensions extensions) {
        if (extensions != null && !extensions.isEmpty()) {
            builder.addString("extensions", extensions.asParameter());
        }
        return builder;
    }
}
