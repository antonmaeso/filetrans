package com.ant.filetrans.metadata;

import com.ant.filetrans.metadata.api.FileMetadata;
import com.ant.filetrans.metadata.api.MetadataKeys;
import com.ant.filetrans.metadata.domain.MetadataCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:metadatae2e;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.repository.type=jdbc"
})
class MetadataAnalyzeE2ETest {

    private static final String ROOT_SHA256 = "4813494d137e1631bba301d5acab6e7bb7aa74ce1185d456565ef51d737677b2";
    private static final String LEAF_SHA256 = "9f91161f43433e49a6de6db680d79f60159f2e4ac9172621a12846428158440b";

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WebApplicationContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setupBatchSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/batch/core/schema-h2.sql")
        );
        populator.execute(dataSource);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void analyzeEndpointProcessesNestedFolders() throws Exception {
        Path targetBaseDir = Files.createDirectory(tempDir.resolve("library"));
        Path rootFile = Files.writeString(targetBaseDir.resolve("root.jpg"), "root");
        Path nestedDir = Files.createDirectories(targetBaseDir.resolve("nested").resolve("deep"));
        Path nestedFile = Files.writeString(nestedDir.resolve("leaf.jpg"), "leaf");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/metadata/analyze")
                        .param("targetBaseDir", targetBaseDir.toString()))
                .andExpect(MockMvcResultMatchers.status().isAccepted());

        String rootFingerprint = assertSidecarExists(rootFile);
        String nestedFingerprint = assertSidecarExists(nestedFile);

        Path catalog = targetBaseDir.resolve("metadata_catalog.json");
        waitForFile(catalog);
        MetadataCatalog metadataCatalog = objectMapper.readValue(catalog.toFile(), MetadataCatalog.class);
        assertThat(metadataCatalog.entries())
                .extracting(MetadataCatalog.CatalogEntry::relativePath)
                .contains(
                        rootFile.getFileName().toString(),
                        Path.of("nested", "deep", "leaf.jpg").toString()
                );
        assertThat(metadataCatalog.entries())
                .extracting(MetadataCatalog.CatalogEntry::fingerprintSha256)
                .containsExactlyInAnyOrder(rootFingerprint, nestedFingerprint);
        assertThat(metadataCatalog.entries())
                .extracting(MetadataCatalog.CatalogEntry::captureFingerprint)
                .containsOnlyNulls();
    }

    private String assertSidecarExists(Path file) throws Exception {
        Path sidecar = file.resolveSibling(file.getFileName() + ".metadata.json");
        waitForFile(sidecar);
        assertThat(Files.exists(sidecar)).isTrue();
        FileMetadata metadata = objectMapper.readValue(sidecar.toFile(), FileMetadata.class);
        assertThat(metadata.file()).isEqualTo(file);
        String fingerprint = metadata.get(MetadataKeys.FINGERPRINT_SHA256, String.class);
        assertThat(fingerprint).isNotBlank();
        assertThat(metadata.get(MetadataKeys.CAPTURE_FINGERPRINT, String.class)).isNull();
        if (file.getFileName().toString().equals("root.jpg")) {
            assertThat(fingerprint).isEqualTo(ROOT_SHA256);
        } else if (file.getFileName().toString().equals("leaf.jpg")) {
            assertThat(fingerprint).isEqualTo(LEAF_SHA256);
        }
        return fingerprint;
    }

    private void waitForFile(Path path) throws InterruptedException {
        for (int i = 0; i < 50 && !Files.exists(path); i++) {
            Thread.sleep(100);
        }
    }
}
