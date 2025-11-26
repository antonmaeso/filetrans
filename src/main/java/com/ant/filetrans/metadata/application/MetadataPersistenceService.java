package com.ant.filetrans.metadata.application;

import com.ant.filetrans.metadata.api.FileMetadata;
import com.ant.filetrans.metadata.domain.MetadataCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataPersistenceService {

    private final MetadataWorkService workService;
    private final ObjectMapper objectMapper;

    public void writeMetadata(MetadataProcessingResult result) {
        Path metadataFile = metadataSidecarPath(result.metadata().file());
        try {
            Files.createDirectories(metadataFile.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(metadataFile.toFile(), result.metadata());
            log.info("Wrote metadata sidecar {}", metadataFile);
            workService.markProcessed(result.workItemId());
        } catch (IOException e) {
            log.error("Failed writing metadata for {}", result.metadata().file(), e);
            workService.markFailed(result.workItemId(), e.getMessage());
        }
    }

    public void writeCatalog(Path targetRoot, List<MetadataCatalog.CatalogEntry> entries) {
        MetadataCatalog catalog = new MetadataCatalog(Instant.now(), new ArrayList<>(entries));
        Path catalogPath = targetRoot.resolve("metadata_catalog.json");
        try {
            Files.createDirectories(catalogPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(catalogPath.toFile(), catalog);
            log.info("Wrote metadata catalog at {} ({} entries)", catalogPath, entries.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write catalog at " + catalogPath, e);
        }
    }

    private Path metadataSidecarPath(Path file) {
        return file.resolveSibling(file.getFileName() + ".metadata.json");
    }
}
