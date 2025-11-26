package com.ant.filetrans.metadata.infrastructure.batch;

import com.ant.filetrans.metadata.application.MetadataPersistenceService;
import com.ant.filetrans.metadata.domain.MetadataCatalog;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class MetadataCatalogWriter implements ItemWriter<MetadataCatalog.CatalogEntry>, StepExecutionListener {

    private final MetadataPersistenceService persistenceService;
    private final List<MetadataCatalog.CatalogEntry> buffer = new ArrayList<>();

    @Setter
    @Value("#{jobParameters['targetBaseDir']}")
    private String targetBaseDir;

    @Override
    public void write(Chunk<? extends MetadataCatalog.CatalogEntry> chunk) {
        buffer.addAll(chunk.getItems());
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        buffer.clear();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        try {
            persistenceService.writeCatalog(Path.of(targetBaseDir), buffer);
            log.info("Wrote metadata catalog with {} entries", buffer.size());
            return ExitStatus.COMPLETED;
        } catch (Exception e) {
            log.error("Failed to write metadata catalog", e);
            return ExitStatus.FAILED;
        }
    }
}
