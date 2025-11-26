package com.ant.filetrans.metadata.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetadataWorkItemRepository extends JpaRepository<MetadataWorkItemEntity, Long> {

    Optional<MetadataWorkItemEntity> findByFilePath(String filePath);
}
