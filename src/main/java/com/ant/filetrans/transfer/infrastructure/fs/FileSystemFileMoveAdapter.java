package com.ant.filetrans.transfer.infrastructure.fs;

import com.ant.filetrans.transfer.application.FileMovePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Component
public class FileSystemFileMoveAdapter implements FileMovePort {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 150;

    @Override
    public void move(Path source, Path target) {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source file does not exist: " + source);
        }

        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create parent directories for: " + target, e);
        }

        IOException lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                copyThenDelete(source, target);
                return;
            } catch (IOException e) {
                lastError = e;
                log.warn("Copy/delete attempt {} failed for {} → {}: {}",
                        attempt, source, target, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new RuntimeException(
                "Failed to move file after " + MAX_RETRIES + " copy/delete attempts: " + source + " → " + target,
                lastError
        );
    }

    /**
     * Main strategy: copy to a temp file, verify, atomically rename to final target, then delete source.
     */
    private void copyThenDelete(Path source, Path target) throws IOException {
        // temp file in the same directory as the target so we can do an atomic rename
        Path tempTarget = target.resolveSibling(
                target.getFileName() + ".copytmp-" + UUID.randomUUID()
        );

        try {
            // 1️⃣ Copy with attributes
            Files.copy(
                    source,
                    tempTarget,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
            );

            // 2️⃣ Verify sizes match (quick integrity check)
            long srcSize = Files.size(source);
            long tmpSize = Files.size(tempTarget);

            if (srcSize != tmpSize) {
                throw new IOException("Size mismatch after copy: src=" + srcSize + ", tmp=" + tmpSize);
            }

            // 3️⃣ Move temp into final target atomically if possible
            try {
                Files.move(tempTarget, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                // Fall back to non-atomic rename
                Files.move(tempTarget, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4️⃣ Delete original
            Files.delete(source);

            log.info("Copy-delete successful: {} → {}", source, target);
        } catch (IOException e) {
            // Best effort cleanup of the temp file
            try {
                Files.deleteIfExists(tempTarget);
            } catch (IOException cleanupEx) {
                log.warn("Failed to cleanup temp target {}: {}", tempTarget, cleanupEx.getMessage());
            }
            throw e;
        }
    }
}
