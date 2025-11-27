package com.ant.filetrans.metadata.application;

import com.ant.filetrans.ai.api.AiMetadata;
import com.ant.filetrans.metadata.api.FileMetadata;
import com.ant.filetrans.metadata.api.MetadataKeys;
import com.ant.filetrans.metadata.domain.MetadataCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataPersistenceService {

    private final MetadataWorkService workService;
    private final ObjectMapper objectMapper;

    public void writeMetadata(MetadataProcessingResult result) {
        Path metadataFile = metadataSidecarPath(result.metadata().file());
        try {
            Map<String, Object> mergedAttributes = mergeExistingAttributes(metadataFile, result.metadata().attributes());
            FileMetadata merged = new FileMetadata(
                    result.metadata().file(),
                    result.metadata().size(),
                    result.metadata().contentType(),
                    result.metadata().lastModified(),
                    mergedAttributes
            );
            Files.createDirectories(metadataFile.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(metadataFile.toFile(), merged);
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

    public FileMetadata readMetadata(Path file) {
        Path metadataFile = metadataSidecarPath(file);
        if (!Files.exists(metadataFile)) {
            throw new IllegalStateException("Metadata sidecar not found for " + file);
        }
        try {
            return objectMapper.readValue(metadataFile.toFile(), FileMetadata.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read metadata for " + file, e);
        }
    }

    public FileMetadata readOrCreateMetadata(Path file) {
        Path sidecar = metadataSidecarPath(file);
        if (Files.exists(sidecar)) {
            try {
                return objectMapper.readValue(sidecar.toFile(), FileMetadata.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read sidecar for " + file, e);
            }
        }

        return new FileMetadata(
                file,
                safeSize(file),
                safeContentType(file),
                safeLastModified(file),
                Map.of()
        );
    }

    public void writeMetadata(FileMetadata metadata) {
        Path metadataFile = metadataSidecarPath(metadata.file());
        try {
            Map<String, Object> mergedAttributes = mergeExistingAttributes(metadataFile, metadata.attributes());
            FileMetadata merged = new FileMetadata(
                    metadata.file(),
                    metadata.size(),
                    metadata.contentType(),
                    metadata.lastModified(),
                    mergedAttributes
            );
            Files.createDirectories(metadataFile.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(metadataFile.toFile(), merged);
            log.info("Wrote metadata sidecar {}", metadataFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed writing metadata for " + metadata.file(), e);
        }
    }

    public void updateWithAiMetadata(Path target, AiMetadata aiMetadata) {
        FileMetadata current = readOrCreateMetadata(target);
        Map<String, Object> merged = new HashMap<>(current.attributes() == null ? Map.of() : current.attributes());
        merged.put(MetadataKeys.AI_DESCRIPTION, aiMetadata.shortDescription());
        merged.put(MetadataKeys.AI_TAGS, String.join(",", aiMetadata.tags() == null ? List.of() : aiMetadata.tags()));
        merged.put(MetadataKeys.AI_CONFIDENCE, Double.toString(aiMetadata.confidence()));

        FileMetadata updated = new FileMetadata(
                current.file(),
                current.size(),
                current.contentType(),
                current.lastModified(),
                Map.copyOf(merged)
        );
        writeMetadata(updated);
        log.info("Updated sidecar {} with AI metadata", target);
    }

    private Path metadataSidecarPath(Path file) {
        return file.resolveSibling(file.getFileName() + ".metadata.json");
    }

    private Map<String, Object> mergeExistingAttributes(Path metadataFile, Map<String, Object> incoming) {
        Map<String, Object> merged = new HashMap<>(incoming == null ? Map.of() : incoming);
        if (Files.exists(metadataFile)) {
            try {
                FileMetadata existing = objectMapper.readValue(metadataFile.toFile(), FileMetadata.class);
                if (existing.attributes() != null) {
                    merged.putAll(existing.attributes()); // existing values fill gaps
                    merged.putAll(incoming == null ? Map.of() : incoming); // incoming overrides
                }
            } catch (IOException e) {
                log.warn("Failed to merge existing metadata at {}, writing incoming only", metadataFile, e);
            }
        }
        return Map.copyOf(merged);
    }

    private long safeSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0L;
        }
    }

    private String safeContentType(Path file) {
        try {
            return Files.probeContentType(file);
        } catch (IOException e) {
            return null;
        }
    }

    private Instant safeLastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            return Instant.now();
        }
    }
}
