package com.ant.filetrans.ai.web;

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

import com.ant.filetrans.ai.api.model.AnalyzeAiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration test for AI API endpoints.
 * Tests complete HTTP request/response flow with real Spring context.
 * 
 * Validates: Requirements 6.1, 6.2, 9.1, 9.7
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:aiapi;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.repository.type=jdbc"
})
class AiApiIntegrationTest {

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
    void postAiAnalyzeTriggersAnalysisAndReturns200() throws Exception {
        Path imageFile = Files.writeString(tempDir.resolve("test.jpg"), "test image content");

        MvcResult result = mockMvc.perform(post("/ai/analyze")
                        .param("path", imageFile.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(imageFile.toString()))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AnalyzeAiResponse response = objectMapper.readValue(responseBody, AnalyzeAiResponse.class);

        assertThat(response.getMessage()).isNotBlank();
        assertThat(response.getPath()).isEqualTo(imageFile.toString());
    }

    @Test
    void postAiAnalyzeAcceptsJpegExtension() throws Exception {
        Path imageFile = Files.writeString(tempDir.resolve("test.jpeg"), "test image content");

        mockMvc.perform(post("/ai/analyze")
                        .param("path", imageFile.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(imageFile.toString()));
    }

    @Test
    void postAiAnalyzeAcceptsCaseInsensitiveExtensions() throws Exception {
        Path imageFile1 = Files.writeString(tempDir.resolve("test1.JPG"), "test image content");
        Path imageFile2 = Files.writeString(tempDir.resolve("test2.JPEG"), "test image content");

        mockMvc.perform(post("/ai/analyze")
                        .param("path", imageFile1.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/ai/analyze")
                        .param("path", imageFile2.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void postAiAnalyzeReturns400WhenPathMissing() throws Exception {
        mockMvc.perform(post("/ai/analyze"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postAiAnalyzeReturns400ForNonJpgFile() throws Exception {
        Path imageFile = Files.writeString(tempDir.resolve("test.png"), "test image content");

        mockMvc.perform(post("/ai/analyze")
                        .param("path", imageFile.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jsonStructureMatchesOpenApiSchema() throws Exception {
        Path imageFile = Files.writeString(tempDir.resolve("schema-test.jpg"), "test image content");

        MvcResult result = mockMvc.perform(post("/ai/analyze")
                        .param("path", imageFile.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AnalyzeAiResponse response = objectMapper.readValue(responseBody, AnalyzeAiResponse.class);

        // Verify all required fields from OpenAPI schema are present
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getPath()).isNotNull();
    }
}
