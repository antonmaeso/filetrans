package com.ant.filetrans.metadata.application;

import com.ant.filetrans.metadata.domain.MetadataWorkItem;
import com.ant.filetrans.metadata.domain.MetadataWorkStatus;
import com.ant.filetrans.metadata.infrastructure.persistence.MetadataWorkItemEntity;
import com.ant.filetrans.metadata.infrastructure.persistence.MetadataWorkItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataWorkService {

    private final MetadataWorkItemRepository repository;

    public MetadataWorkItem enqueue(Path file) {
        String filePath = file.toString();
        MetadataWorkItemEntity entity = repository.findByFilePath(filePath)
                .orElseGet(() -> repository.save(new MetadataWorkItemEntity(filePath, MetadataWorkStatus.PENDING)));

        if (entity.getStatus() != MetadataWorkStatus.PENDING) {
            entity.setStatus(MetadataWorkStatus.PENDING);
            entity.setUpdatedAt(Instant.now());
            entity.setErrorMessage(null);
            entity = repository.save(entity);
        }

        MetadataWorkItem workItem = toDomain(entity);
        log.info("Enqueued metadata work {} with status {}", workItem.file(), workItem.status());
        return workItem;
    }

    public void markProcessed(Long id) {
        repository.findById(id).ifPresent(entity -> {
            entity.setStatus(MetadataWorkStatus.PROCESSED);
            entity.setUpdatedAt(Instant.now());
            entity.setErrorMessage(null);
            repository.save(entity);
            log.info("Marked metadata work {} as PROCESSED", entity.getFilePath());
        });
    }

    public void markFailed(Long id, String error) {
        repository.findById(id).ifPresent(entity -> {
            entity.setStatus(MetadataWorkStatus.FAILED);
            entity.setErrorMessage(error);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
            log.warn("Marked metadata work {} as FAILED: {}", entity.getFilePath(), error);
        });
    }

    private MetadataWorkItem toDomain(MetadataWorkItemEntity entity) {
        return new MetadataWorkItem(
                entity.getId(),
                Path.of(entity.getFilePath()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getErrorMessage()
        );
    }
}
