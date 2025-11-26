package com.ant.filetrans.metadata.domain;

import java.nio.file.Path;
import java.time.Instant;

public record MetadataWorkItem(
        Long id,
        Path file,
        MetadataWorkStatus status,
        Instant createdAt,
        Instant updatedAt,
        String errorMessage
) {}
