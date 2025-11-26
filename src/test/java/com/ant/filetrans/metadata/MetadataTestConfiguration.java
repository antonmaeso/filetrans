package com.ant.filetrans.metadata;

import com.ant.filetrans.metadata.application.MetadataWorkService;
import com.ant.filetrans.metadata.domain.MetadataWorkItem;
import com.ant.filetrans.metadata.domain.MetadataWorkStatus;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@TestConfiguration
public class MetadataTestConfiguration {

    @Bean
    public RecordingMetadataWorkService metadataWorkService() {
        return new RecordingMetadataWorkService();
    }

    public static class RecordingMetadataWorkService extends MetadataWorkService {

        private Long processedId;
        private Long failedId;
        private String lastError;

        public RecordingMetadataWorkService() {
            super(null);
        }

        @Override
        public MetadataWorkItem enqueue(Path file) {
            return new MetadataWorkItem(
                    1L,
                    file,
                    MetadataWorkStatus.PENDING,
                    Instant.now(),
                    Instant.now(),
                    null
            );
        }

        @Override
        public void markProcessed(Long id) {
            this.processedId = id;
        }

        @Override
        public void markFailed(Long id, String error) {
            this.failedId = id;
            this.lastError = error;
        }

        public Optional<Long> lastProcessedId() {
            return Optional.ofNullable(processedId);
        }

        public Optional<Long> lastFailedId() {
            return Optional.ofNullable(failedId);
        }

        public Optional<String> lastErrorMessage() {
            return Optional.ofNullable(lastError);
        }
    }
}
