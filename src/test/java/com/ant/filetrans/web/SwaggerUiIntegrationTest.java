package com.ant.filetrans.web;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration test for Swagger UI and OpenAPI documentation endpoints.
 * Tests that OpenAPI documentation is served correctly.
 * 
 * Validates: Requirements 11.1, 11.2, 11.3
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:swagger;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.repository.type=jdbc"
})
class SwaggerUiIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setupBatchSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/batch/core/schema-h2.sql")
        );
        populator.execute(dataSource);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void apiDocsEndpointServesOpenApiJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode apiDocs = objectMapper.readTree(responseBody);

        // Verify it's a valid OpenAPI document
        assertThat(apiDocs.has("openapi")).isTrue();
        assertThat(apiDocs.get("openapi").asText()).startsWith("3.");
        assertThat(apiDocs.has("paths")).isTrue();
    }

    @Test
    void apiDocsIncludesTransferEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode apiDocs = objectMapper.readTree(responseBody);
        JsonNode paths = apiDocs.get("paths");

        // Verify Transfer API endpoints are documented
        assertThat(paths.has("/transfers")).isTrue();
        assertThat(paths.get("/transfers").has("post")).isTrue();
        assertThat(paths.has("/transfers/{executionId}")).isTrue();
        assertThat(paths.get("/transfers/{executionId}").has("get")).isTrue();
    }

    @Test
    void apiDocsIncludesMetadataEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode apiDocs = objectMapper.readTree(responseBody);
        JsonNode paths = apiDocs.get("paths");

        // Verify Metadata API endpoints are documented
        assertThat(paths.has("/api/metadata/analyze")).isTrue();
        assertThat(paths.get("/api/metadata/analyze").has("post")).isTrue();
    }

    @Test
    void apiDocsIncludesAiEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode apiDocs = objectMapper.readTree(responseBody);
        JsonNode paths = apiDocs.get("paths");

        // Verify AI API endpoints are documented
        assertThat(paths.has("/ai/analyze")).isTrue();
        assertThat(paths.get("/ai/analyze").has("post")).isTrue();
    }

    @Test
    void swaggerUiEndpointIsAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void swaggerUiIndexIsAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsIncludesSchemaDefinitions() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode apiDocs = objectMapper.readTree(responseBody);
        JsonNode components = apiDocs.get("components");

        assertThat(components).isNotNull();
        assertThat(components.has("schemas")).isTrue();
        
        JsonNode schemas = components.get("schemas");
        
        // Verify key schemas are present
        assertThat(schemas.has("CreateTransferRequest")).isTrue();
        assertThat(schemas.has("TransferResponse")).isTrue();
        assertThat(schemas.has("AnalyzeMetadataResponse")).isTrue();
        assertThat(schemas.has("AnalyzeAiResponse")).isTrue();
    }
}
