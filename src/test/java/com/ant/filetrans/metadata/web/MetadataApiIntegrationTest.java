package com.ant.filetrans.metadata.web;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ant.filetrans.metadata.api.model.AnalyzeMetadataResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration test for Metadata API endpoints.
 * Tests complete HTTP request/response flow with real Spring context.
 * 
 * Validates: Requirements 5.1, 5.2, 9.1, 9.7
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:metadataapi;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.repository.type=jdbc"
})
class MetadataApiIntegrationTest {

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
    void postMetadataAnalyzeTriggersAnalysisAndReturns202() throws Exception {
        Path targetBaseDir = Files.createDirectory(tempDir.resolve("library"));
        Files.writeString(targetBaseDir.resolve("test.jpg"), "test content");

        MvcResult result = mockMvc.perform(post("/api/metadata/analyze")
                        .param("targetBaseDir", targetBaseDir.toString()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.targetBaseDir").value(targetBaseDir.toString()))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AnalyzeMetadataResponse response = objectMapper.readValue(responseBody, AnalyzeMetadataResponse.class);

        assertThat(response.getMessage()).isNotBlank();
        assertThat(response.getTargetBaseDir()).isEqualTo(targetBaseDir.toString());
    }

    @Test
    void postMetadataAnalyzeReturns400WhenTargetBaseDirMissing() throws Exception {
        mockMvc.perform(post("/api/metadata/analyze"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jsonStructureMatchesOpenApiSchema() throws Exception {
        Path targetBaseDir = Files.createDirectory(tempDir.resolve("library2"));
        Files.writeString(targetBaseDir.resolve("test.jpg"), "test content");

        MvcResult result = mockMvc.perform(post("/api/metadata/analyze")
                        .param("targetBaseDir", targetBaseDir.toString()))
                .andExpect(status().isAccepted())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AnalyzeMetadataResponse response = objectMapper.readValue(responseBody, AnalyzeMetadataResponse.class);

        // Verify all required fields from OpenAPI schema are present
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getTargetBaseDir()).isNotNull();
    }

    @Test
    void postMetadataAnalyzeWithNestedDirectories() throws Exception {
        Path targetBaseDir = Files.createDirectory(tempDir.resolve("library3"));
        Path nestedDir = Files.createDirectories(targetBaseDir.resolve("nested").resolve("deep"));
        Files.writeString(nestedDir.resolve("nested.jpg"), "nested content");

        mockMvc.perform(post("/api/metadata/analyze")
                        .param("targetBaseDir", targetBaseDir.toString()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.targetBaseDir").value(targetBaseDir.toString()));
    }
}
