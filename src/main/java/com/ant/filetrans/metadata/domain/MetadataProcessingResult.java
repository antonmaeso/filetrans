package com.ant.filetrans.metadata.domain;

public record MetadataProcessingResult(
        Long workItemId,
        FileMetadata metadata
) {}
