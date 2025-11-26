package com.ant.filetrans.metadata.application;

import com.ant.filetrans.metadata.domain.FileMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Slf4j
@Service
public class FileMetadataService {

    public FileMetadata capture(Path file) throws IOException {
        long size = Files.size(file);
        String contentType = Files.probeContentType(file);
        Instant lastModified = Files.getLastModifiedTime(file).toInstant();
        return new FileMetadata(file, size, contentType, lastModified);
    }
}
