package com.ant.filetrans.metadata.application.extract;

import com.ant.filetrans.metadata.api.FileMetadata;

import java.nio.file.Path;

public interface MetadataExtractor {

    FileMetadata extract(Path file);
}
