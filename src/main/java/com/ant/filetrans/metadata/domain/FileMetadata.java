package com.ant.filetrans.metadata.domain;

import java.nio.file.Path;
import java.time.Instant;

public record FileMetadata(
        Path file,
        long size,
        String contentType,
        Instant lastModified
) {}
