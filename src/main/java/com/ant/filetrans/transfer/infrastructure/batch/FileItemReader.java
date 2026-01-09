package com.ant.filetrans.transfer.infrastructure.batch;

import com.ant.filetrans.transfer.domain.FileDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Slf4j
public class FileItemReader implements ItemStreamReader<FileDescriptor> {

    private static final String CURRENT_INDEX_KEY = "filetrans.reader.index";

    private final Path sourceDir;
    private final Extensions extensions;

    private List<Path> files = List.of();
    private int currentIndex;

    public FileItemReader(String sourceDir, Extensions extensions) {
        this.sourceDir = Path.of(sourceDir);
        if (!Files.exists(this.sourceDir)) {
            throw new IllegalArgumentException("Source directory does not exist: " + this.sourceDir);
        }
        this.extensions = extensions == null ? Extensions.parse(null) : extensions;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            this.files = Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .filter(extensions::accepts)
                    .sorted()
                    .toList();

            this.currentIndex = 0;

            log.info("FileItemReader opened {} files for {}, resuming at index {} (extensions={})",
                    files.size(), sourceDir, currentIndex, extensions.values());
        } catch (IOException e) {
            throw new ItemStreamException("Failed to scan directory: " + sourceDir, e);
        }
    }

    @Override
    public FileDescriptor read() {
        if (currentIndex >= files.size()) {
            log.debug("FileItemReader exhausted for {}", sourceDir);
            return null;
        }

        Path path = files.get(currentIndex++);
        try {
            Instant lastModified = Files.getLastModifiedTime(path).toInstant();
            log.debug("FileItemReader returning {}", path);
            return new FileDescriptor(path, lastModified);
        } catch (IOException e) {
            log.warn("Skipping unreadable file {} ({})", path, e.getMessage());
            return read();
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext != null) {
            executionContext.putInt(CURRENT_INDEX_KEY, currentIndex);
        }
    }

    @Override
    public void close() {
        files = List.of();
        currentIndex = 0;
    }


}
