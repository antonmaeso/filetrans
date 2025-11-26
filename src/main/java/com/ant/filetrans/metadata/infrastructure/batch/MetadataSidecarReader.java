package com.ant.filetrans.metadata.infrastructure.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class MetadataSidecarReader implements ItemStreamReader<Path> {

    private final Path root;
    private List<Path> files = List.of();
    private int index;

    public MetadataSidecarReader(Path root) {
        this.root = root;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            this.files = Files.walk(root)
                    .filter(p -> p.getFileName().toString().endsWith(".metadata.json"))
                    .sorted()
                    .toList();
            this.index = 0;
            log.info("MetadataSidecarReader found {} metadata files under {}", files.size(), root);
        } catch (IOException e) {
            throw new ItemStreamException("Failed to scan metadata sidecars under " + root, e);
        }
    }

    @Override
    public Path read() {
        if (index >= files.size()) {
            return null;
        }
        return files.get(index++);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // no-op
    }

    @Override
    public void close() {
        files = List.of();
        index = 0;
    }
}
