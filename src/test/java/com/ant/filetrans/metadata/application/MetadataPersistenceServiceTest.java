package com.ant.filetrans.metadata.application;

import com.ant.filetrans.metadata.MetadataConfig;
import com.ant.filetrans.metadata.MetadataTestConfiguration;
import com.ant.filetrans.metadata.domain.FileMetadata;
import com.ant.filetrans.metadata.domain.MetadataCatalog;
import com.ant.filetrans.metadata.domain.MetadataProcessingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {MetadataConfig.class, MetadataPersistenceService.class, MetadataTestConfiguration.class})
class MetadataPersistenceServiceTest {

    @Autowired
    private MetadataPersistenceService persistenceService;

    @Autowired
    private MetadataWorkService workService;

    @TempDir
    Path tempDir;

    @Test
    void writesMetadataSidecarAndMarksProcessed() throws Exception {
        Path file = Files.writeString(tempDir.resolve("image.jpg"), "test");
        FileMetadata metadata = new FileMetadata(file, 4, "text/plain", Instant.now());
        MetadataProcessingResult result = new MetadataProcessingResult(42L, metadata);

        persistenceService.writeMetadata(result);

        Path sidecar = tempDir.resolve("image.jpg.metadata.json");
        assertThat(sidecar).exists();

        String json = Files.readString(sidecar);
        assertThat(json).contains("text/plain");
        verify(workService).markProcessed(42L);
    }

    @Test
    void writesCatalogFile() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("catalog"));

        MetadataCatalog.CatalogEntry entry = new MetadataCatalog.CatalogEntry(
                "image.jpg", 4, "text/plain", Instant.now());

        persistenceService.writeCatalog(root, List.of(entry));

        Path catalog = root.resolve("metadata_catalog.json");
        assertThat(catalog).exists();
        String json = Files.readString(catalog);
        assertThat(json).contains("image.jpg");
    }
}
