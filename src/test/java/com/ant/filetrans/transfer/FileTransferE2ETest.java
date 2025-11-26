package com.ant.filetrans.transfer;

import com.ant.filetrans.metadata.domain.FileMetadata;
import com.ant.filetrans.metadata.domain.MetadataCatalog;
import com.ant.filetrans.transfer.application.FileTransferService;
import com.ant.filetrans.transfer.infrastructure.batch.Extensions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.repository.type=jdbc"
})
class FileTransferE2ETest {

    @Autowired
    private FileTransferService fileTransferService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @TempDir
    Path tempDir;

    @org.junit.jupiter.api.BeforeEach
    void setupBatchSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/batch/core/schema-h2.sql")
        );
        populator.execute(dataSource);
    }

    @Test
    void movesFileAndCreatesMetadata() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target"));

        Path photo = sourceDir.resolve("photo.jpg");
        Files.writeString(photo, "demo");
        Instant lastModified = LocalDate.of(2024, 3, 30)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        Files.setLastModifiedTime(photo, FileTime.from(lastModified));

        JobExecution execution = fileTransferService.transferDirectory(
                sourceDir,
                targetDir,
                Extensions.of(Set.of("jpg"))
        );

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Path expectedLocation = targetDir.resolve(Path.of("2024", "2024-03-30", "photo.jpg"));
        waitForFile(expectedLocation);
        assertThat(Files.exists(expectedLocation)).isTrue();
        assertThat(Files.notExists(photo)).isTrue();

        Path sidecar = expectedLocation.getParent().resolve("photo.jpg.metadata.json");
        waitForFile(sidecar);
        FileMetadata metadata = objectMapper.readValue(sidecar.toFile(), FileMetadata.class);
        assertThat(metadata.file()).isEqualTo(expectedLocation);

        Path catalog = targetDir.resolve("metadata_catalog.json");
        waitForFile(catalog);
        MetadataCatalog metadataCatalog = objectMapper.readValue(catalog.toFile(), MetadataCatalog.class);
        assertThat(metadataCatalog.entries())
                .extracting(MetadataCatalog.CatalogEntry::relativePath)
                .contains("2024/2024-03-30/photo.jpg");
    }

    private void waitForFile(Path file) throws InterruptedException {
        for (int i = 0; i < 50 && !Files.exists(file); i++) {
            Thread.sleep(100);
        }
    }
}
