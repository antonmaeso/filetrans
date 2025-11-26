package com.ant.filetrans.metadata.application;

import com.ant.filetrans.metadata.api.FileMetadata;

public record MetadataProcessingResult(
        Long workItemId,
        FileMetadata metadata
) {}
