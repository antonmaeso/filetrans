package com.ant.filetrans.transfer.web;

import com.ant.filetrans.transfer.api.model.CreateTransferRequest;
import com.ant.filetrans.transfer.api.model.TransferResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Transfer API endpoints.
 * Tests complete HTTP request/response flow with real Spring context.
 * 
 * Validates: Requirements 4.1, 4.2, 4.4, 4.5, 9.1, 9.7
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:transferapi;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.repository.type=jdbc"
})
class TransferApiIntegrationTest {

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
    void postTransfersCreatesJobAndReturns202() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target"));
        Files.writeString(sourceDir.resolve("test.jpg"), "test content");

        CreateTransferRequest request = new CreateTransferRequest()
                .sourceDir(sourceDir.toString())
                .targetBaseDir(targetDir.toString());

        MvcResult result = mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").isNumber())
                .andExpect(jsonPath("$.status").isString())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TransferResponse response = objectMapper.readValue(responseBody, TransferResponse.class);
        
        assertThat(response.getExecutionId()).isNotNull();
        assertThat(response.getStatus()).isNotNull();
    }

    @Test
    void getTransfersRetrievesJobStatus() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source2"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target2"));
        Files.writeString(sourceDir.resolve("test.jpg"), "test content");

        CreateTransferRequest request = new CreateTransferRequest()
                .sourceDir(sourceDir.toString())
                .targetBaseDir(targetDir.toString());

        MvcResult createResult = mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        TransferResponse createResponse = objectMapper.readValue(createResponseBody, TransferResponse.class);
        Long executionId = createResponse.getExecutionId();

        mockMvc.perform(get("/transfers/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.status").isString());
    }

    @Test
    void getTransfersReturns404ForNonExistentJob() throws Exception {
        mockMvc.perform(get("/transfers/{executionId}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void postTransfersWithSingleFileCreatesJob() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source3"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target3"));
        Path singleFile = Files.writeString(sourceDir.resolve("single.jpg"), "single file");

        CreateTransferRequest request = new CreateTransferRequest()
                .filePath(singleFile.toString())
                .targetBaseDir(targetDir.toString());

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").isNumber())
                .andExpect(jsonPath("$.status").isString());
    }

    @Test
    void jsonStructureMatchesOpenApiSchema() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source4"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target4"));
        Files.writeString(sourceDir.resolve("test.jpg"), "test content");

        CreateTransferRequest request = new CreateTransferRequest()
                .sourceDir(sourceDir.toString())
                .targetBaseDir(targetDir.toString())
                .extensions(Arrays.asList("jpg", "jpeg"));

        MvcResult result = mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TransferResponse response = objectMapper.readValue(responseBody, TransferResponse.class);

        // Verify all required fields from OpenAPI schema are present
        assertThat(response.getExecutionId()).isNotNull();
        assertThat(response.getStatus()).isNotNull();
        // startTime and endTime are optional
    }
}
