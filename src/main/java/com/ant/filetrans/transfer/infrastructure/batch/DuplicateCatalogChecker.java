package com.ant.filetrans.transfer.infrastructure.batch;

import com.ant.filetrans.transfer.domain.MovedFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DuplicateCatalogChecker {

    private static final HexFormat HEX = HexFormat.of();

    private final Path targetBaseDir;
    private final ObjectMapper objectMapper;
    private final Map<String, String> existingByHash;

    public DuplicateCatalogChecker(Path targetBaseDir, ObjectMapper objectMapper) {
        this.targetBaseDir = targetBaseDir;
        this.objectMapper = objectMapper;
        this.existingByHash = loadCatalog();
    }

    public boolean isDuplicate(MovedFile movedFile) {
        String hash = hash(movedFile.source());
        String existingPath = existingByHash.get(hash);
        if (existingPath != null) {
            log.warn("Skipping move for {} -> {}: matching hash already present at {}",
                    movedFile.source(),
                    relativeTarget(movedFile.target()),
                    existingPath);
            return true;
        }
        return false;
    }

    private String relativeTarget(Path target) {
        try {
            return targetBaseDir.relativize(target).toString();
        } catch (IllegalArgumentException e) {
            return target.toString();
        }
    }

    private Map<String, String> loadCatalog() {
        Path catalogPath = targetBaseDir.resolve("metadata_catalog.json");
        if (!Files.exists(catalogPath)) {
            log.debug("No metadata catalog found at {}, treating as empty", catalogPath);
            return Map.of();
        }
        try {
            CatalogSnapshot snapshot = objectMapper.readValue(catalogPath.toFile(), CatalogSnapshot.class);
            if (snapshot.entries() == null) {
                return Map.of();
            }
            return snapshot.entries().stream()
                    .filter(e -> e.fingerprintSha256() != null && !e.fingerprintSha256().isBlank())
                    .collect(Collectors.toMap(CatalogEntry::fingerprintSha256, CatalogEntry::relativePath, (a, b) -> a));
        } catch (IOException e) {
            log.warn("Failed to read metadata catalog at {}, proceeding without duplicate detection", catalogPath, e);
            return Map.of();
        }
    }

    private String hash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (var in = Files.newInputStream(file)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HEX.formatHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash file " + file, e);
        }
    }

    private record CatalogSnapshot(List<CatalogEntry> entries) {}
    private record CatalogEntry(String relativePath, String metadataSidecar, String fingerprintSha256, String captureFingerprint) {}
}
