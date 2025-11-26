package com.ant.filetrans.transfer.infrastructure.batch;

import com.ant.filetrans.transfer.domain.TransferJobCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferJobCompletionPublisher implements JobExecutionListener {

    private final ApplicationEventPublisher events;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (!"photoImportJob".equals(jobExecution.getJobInstance().getJobName())) {
            return;
        }
        if (!jobExecution.getStatus().isUnsuccessful()) {
            String targetBaseDir = jobExecution.getJobParameters().getString(FileTransferConfig.BASE_DIR_PARAM, null);
            if (targetBaseDir != null) {
                log.info("Publishing TransferJobCompletedEvent for {}", targetBaseDir);
                events.publishEvent(new TransferJobCompletedEvent(targetBaseDir));
            }
        }
    }
}
