package com.ant.filetrans.metadata.infrastructure.persistence;

import com.ant.filetrans.metadata.domain.MetadataWorkStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "metadata_work_items", indexes = {
        @Index(name = "idx_metadata_file", columnList = "filePath", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class MetadataWorkItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filePath;

    @Enumerated(EnumType.STRING)
    private MetadataWorkStatus status = MetadataWorkStatus.PENDING;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    private String errorMessage;

    public MetadataWorkItemEntity(String filePath, MetadataWorkStatus status) {
        this.filePath = filePath;
        this.status = status;
    }
}
