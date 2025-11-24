package com.ant.filetrans.transfer.infrastructure.batch;

import com.ant.filetrans.transfer.domain.FileDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class FileItemReader implements ItemReader<FileDescriptor> {

    private final Iterator<Path> iterator;
    private final Set<String> extensions;

    public FileItemReader(String sourceDir, Set<String> extensions) {
        Path dir = Path.of(sourceDir);
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Source directory does not exist: " + dir);
        }
        this.extensions = extensions == null ? Set.of() : extensions;
        this.iterator = loadFiles(dir, this.extensions).iterator();
        log.info("FileItemReader prepared for {}, available={}", dir, this.iterator.hasNext());
    }

    @Override
    public FileDescriptor read() {
        if (iterator == null || !iterator.hasNext()) {
            log.debug("FileItemReader exhausted");
            return null; // Spring Batch signals end of input
        }

        Path path = iterator.next();

        try {
            Instant lastModified = Files.getLastModifiedTime(path).toInstant();
            log.debug("FileItemReader returning {}", path);
            return new FileDescriptor(path, lastModified);
        } catch (IOException e) {
            // Skip unreadable file, but do NOT fail the whole job
            log.warn("Skipping unreadable file {} ({})", path, e.getMessage());
            return read(); // continue to next file
        }
    }

    private static List<Path> loadFiles(Path dir, Set<String> extensions) {
        try {
            try (Stream<Path> stream = Files.walk(dir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(path -> extensions.isEmpty() || extensions.contains(extensionOf(path)))
                        .sorted()
                        .toList();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan directory: " + dir, ex);
        }
    }

    private static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase();
    }
}
