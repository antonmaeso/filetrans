package com.ant.filetrans.metadata.domain;

import java.time.Instant;
import java.util.List;

public record MetadataCatalog(
        Instant generatedAt,
        List<CatalogEntry> entries
) {
    public record CatalogEntry(
            String relativePath,
            String metadataSidecar,
            String fingerprintSha256,
            String captureFingerprint
    ) {}
}
