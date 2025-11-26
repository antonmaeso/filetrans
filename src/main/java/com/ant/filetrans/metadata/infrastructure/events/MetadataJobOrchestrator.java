package com.ant.filetrans.metadata.infrastructure.events;

import com.ant.filetrans.transfer.domain.TransferJobCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataJobOrchestrator {

    private final JobOperator jobOperator;
    private final Job metadataExtractionJob;
    private final Job metadataCatalogJob;

    @EventListener
    public void onTransferCompleted(TransferJobCompletedEvent event) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("targetBaseDir", event.targetBaseDir(), false)
                .addLong("timestamp", System.currentTimeMillis(), false)
                .addString("run.id", java.util.UUID.randomUUID().toString(), true)
                .toJobParameters();

        log.info("Metadata job parameters: targetBaseDir={}, timestamp={}, run.id={}",
                event.targetBaseDir(), params.getLong("timestamp"), params.getString("run.id"));
        log.info("Starting metadataExtractionJob for {}, uuid {}", event.targetBaseDir(), params.getString("run.id"));
        JobExecution extraction = jobOperator.start(metadataExtractionJob, params);
        log.info("Metadata extraction job finished with status {}", extraction.getStatus());
        if (extraction.getStatus().isUnsuccessful()) {
            log.warn("Metadata extraction job failed: {}", extraction.getStatus());
            return;
        }

        log.info("Starting metadataCatalogJob for {}", event.targetBaseDir());
        JobExecution catalog = jobOperator.start(metadataCatalogJob, params);
        log.info("Metadata catalog job finished with status {}", catalog.getStatus());
    }
}
