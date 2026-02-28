package com.ant.filetrans.transfer.application;

import com.ant.filetrans.transfer.infrastructure.batch.Extensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferService {

    public static final String SOURCE_DIR = "sourceDir";
    public static final String BASE_DIR = "targetBaseDir";
    public static final String TIMESTAMP = "timestamp";
    public static final String FILE_PATH = "filePath";

    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final Job photoImportJob;

    public JobExecution transferDirectory(Path sourceDir, Path targetBaseDir, Extensions extensions) throws Exception {
        log.info("Starting directory transfer job from {} to {} with extensions {}", sourceDir, targetBaseDir, extensions);

        JobParametersBuilder builder = new JobParametersBuilder()
                .addString(SOURCE_DIR, sourceDir.toString(), true)
                .addString(BASE_DIR, targetBaseDir.toString(), true)
                .addLong(TIMESTAMP, System.currentTimeMillis(), true)
                .addString("run.id", UUID.randomUUID().toString(), true);
        addExtensions(builder, extensions);
        JobParameters params = builder.toJobParameters();

        JobExecution execution = jobLauncher.run(photoImportJob, params);
        log.info("Directory transfer job {} started", execution.getId());
        return execution;
    }

    public JobExecution transferSingleFile(Path file, Path targetBaseDir, Extensions extensions) throws Exception {
        log.info("Starting single file transfer for {} -> {} with extensions {}", file, targetBaseDir, extensions);

        JobParametersBuilder builder = new JobParametersBuilder()
                .addString(SOURCE_DIR, file.getParent().toString(), true)
                .addString(FILE_PATH, file.toString(), true)
                .addString(BASE_DIR, targetBaseDir.toString(), true)
                .addLong(TIMESTAMP, System.currentTimeMillis(), true)
                .addString("run.id", UUID.randomUUID().toString(), true);
        addExtensions(builder, extensions);
        JobParameters params = builder.toJobParameters();

        JobExecution execution = jobLauncher.run(photoImportJob, params);
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

        if (!execution.isRunning()) {
            log.warn("Attempted to stop job execution {} but it is not running", executionId);
            throw new JobExecutionNotRunningException("JobExecution " + executionId + " is not running");
        }

        log.info("Stopping job execution {}", executionId);
        execution.setStatus(BatchStatus.STOPPING);
        execution.setExitStatus(ExitStatus.STOPPED);
        jobRepository.update(execution);
    }

    /**
     * Get all job executions with pagination support.
     * Returns executions sorted by start time descending (most recent first).
     *
     * @param page zero-based page number
     * @param size number of items per page
     * @return list of job executions for the requested page
     */
    public List<JobExecution> getAllJobExecutions(int page, int size) {
        log.debug("Fetching job executions page {} with size {}", page, size);
        
        // Get all job instances for our photoImportJob
        List<org.springframework.batch.core.job.JobInstance> jobInstances = 
                jobRepository.findJobInstances(photoImportJob.getName());
        
        // Collect all executions from all job instances
        List<JobExecution> allExecutions = jobInstances.stream()
                .flatMap(instance -> jobRepository.findJobExecutions(instance).stream())
                .sorted(Comparator.comparing(
                        JobExecution::getStartTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, allExecutions.size());
        
        if (start >= allExecutions.size()) {
            return List.of();
        }
        
        return allExecutions.subList(start, end);
    }

    /**
     * Get total count of job executions.
     *
     * @return total number of job executions
     */
    public long getTotalJobExecutionCount() {
        List<org.springframework.batch.core.job.JobInstance> jobInstances = 
                jobRepository.findJobInstances(photoImportJob.getName());
        
        return jobInstances.stream()
                .mapToLong(instance -> jobRepository.findJobExecutions(instance).size())
                .sum();
    }

    /**
     * Delete a job execution and its related records.
     * Note: This does NOT delete the transferred files, only the job execution records.
     *
     * @param executionId the job execution ID to delete
     * @throws IllegalStateException if the job is currently running
     */
    public void deleteJobExecution(long executionId) {
        JobExecution execution = jobRepository.getJobExecution(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("JobExecution " + executionId + " not found");
        }

        if (execution.isRunning()) {
            throw new IllegalStateException("Cannot delete running job execution " + executionId);
        }

        log.info("Deleting job execution {}", executionId);
        jobRepository.deleteJobExecution(execution);
    }

    /**
     * Retry a failed job execution by creating a new execution with the same parameters.
     *
     * @param executionId the original job execution ID
     * @return the new job execution
     * @throws Exception if job launch fails
     */
    public JobExecution retryJobExecution(long executionId) throws Exception {
        JobExecution originalExecution = jobRepository.getJobExecution(executionId);
        if (originalExecution == null) {
            throw new IllegalArgumentException("JobExecution " + executionId + " not found");
        }

        if (originalExecution.getStatus() != BatchStatus.FAILED) {
            throw new IllegalStateException("Can only retry FAILED jobs, but job " + executionId + " has status " + originalExecution.getStatus());
        }

        log.info("Retrying job execution {}", executionId);
        
        // Extract original parameters
        JobParameters originalParams = originalExecution.getJobParameters();
        
        // Create new parameters with a new run.id to make it unique
        JobParametersBuilder builder = new JobParametersBuilder(originalParams)
                .addString("run.id", UUID.randomUUID().toString(), true)
                .addLong(TIMESTAMP, System.currentTimeMillis(), true);
        
        JobParameters newParams = builder.toJobParameters();
        
        // Launch new job with same parameters
        JobExecution newExecution = jobLauncher.run(photoImportJob, newParams);
        log.info("Retried job {} as new execution {}", executionId, newExecution.getId());
        
        return newExecution;
    }

    private static JobParametersBuilder addExtensions(JobParametersBuilder builder, Extensions extensions) {
        if (extensions != null && !extensions.isEmpty()) {
            builder.addString("extensions", extensions.asParameter());
        }
        return builder;
    }
}
