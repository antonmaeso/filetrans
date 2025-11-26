package com.ant.filetrans.metadata.application;

import com.ant.filetrans.transfer.domain.TransferJobCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecursiveMetadataBootstrapService {

    private final MetadataWorkService workService;
    private final ApplicationEventPublisher eventPublisher;

    public void initializeFromBaseDirectory(Path baseDir) {
        log.info("Starting recursive metadata bootstrap from {}", baseDir);

        int[] count = new int[1];

        try (var paths = Files.walk(baseDir)) {
            paths
                    .filter(this::notHidden)       // <--- prevent hidden files AND hidden dirs
                    .filter(Files::isRegularFile)  // only process files
                    .forEach(path -> {
                        try {
                            workService.enqueue(path);
                            count[0]++;
                        } catch (Exception e) {
                            log.warn("Failed to enqueue metadata work for {}", path, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory " + baseDir, e);
        }

        log.info("Enqueued {} files for metadata processing under {}", count[0], baseDir);

        eventPublisher.publishEvent(new TransferJobCompletedEvent(baseDir.toString()));
    }

    /**
     * Return true if the path is NOT hidden.
     */
    private boolean notHidden(Path path) {
        try {
            // Exclude macOS .DS_Store explicitly
            if (path.getFileName().toString().equals(".DS_Store")) {
                return false;
            }

            // Exclude any file/directory that is hidden
            return !Files.isHidden(path);

        } catch (IOException e) {
            // If we cannot determine hidden state, assume visible
            log.warn("Could not determine hidden state for {}: {}", path, e.getMessage());
            return true;
        }
    }
}
