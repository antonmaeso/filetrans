package com.ant.filetrans.transfer.application;

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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferService {

    private final JobOperator jobOperator;       // batch 6 operator
    private final JobRepository jobRepository;   // metadata access
    private final Job photoImportJob;            // inject the Job bean directly

    public JobExecution transferDirectory(Path sourceDir, Path targetBaseDir, List<String> extensions) throws Exception {
        log.info("Starting directory transfer job from {} to {} with extensions {}", sourceDir, targetBaseDir, extensions);

        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("sourceDir", sourceDir.toString())
                .addString("targetBaseDir", targetBaseDir.toString())
                .addLong("timestamp", System.currentTimeMillis());
        addExtensions(builder, extensions);
        JobParameters params = builder.toJobParameters();

        JobExecution execution = jobOperator.start(photoImportJob, params);
        log.info("Directory transfer job {} started", execution.getId());
        return execution;
    }

    public JobExecution transferSingleFile(Path file, Path targetBaseDir, List<String> extensions) throws Exception {
        log.info("Starting single file transfer for {} -> {} with extensions {}", file, targetBaseDir, extensions);

        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("sourceDir", file.getParent().toString())
                .addString("filePath", file.toString())
                .addString("targetBaseDir", targetBaseDir.toString())
                .addLong("timestamp", System.currentTimeMillis());
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

    private static JobParametersBuilder addExtensions(JobParametersBuilder builder, List<String> extensions) {
        if (extensions != null && !extensions.isEmpty()) {
            builder.addString("extensions", String.join(",", extensions));
        }
        return builder;
    }
}
