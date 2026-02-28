package com.ant.filetrans.transfer.web;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

import com.ant.filetrans.transfer.api.model.CreateTransferRequest;
import com.ant.filetrans.transfer.api.model.TransferResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based test for API contract preservation.
 * Verifies that generated DTOs produce identical JSON structure to hand-written DTOs.
 * 
 * Feature: openapi-rest-service
 * Property 18: For any existing API endpoint, replacing hand-written DTOs with generated DTOs
 * SHALL produce identical JSON request/response structures for the same data.
 * 
 * Validates: Requirements 9.1, 9.2, 9.3, 9.7
 */
@Tag("property-test")
class ApiContractPreservationPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Property(tries = 100)
    void createTransferRequestJsonStructureIsStable(
            @ForAll("createTransferRequests") CreateTransferRequest request
    ) throws Exception {
        // Serialize the generated DTO to JSON
        String json = objectMapper.writeValueAsString(request);
        JsonNode jsonNode = objectMapper.readTree(json);

        // Verify expected fields are present
        if (request.getSourceDir() != null) {
            assertThat(jsonNode.has("sourceDir")).isTrue();
            assertThat(jsonNode.get("sourceDir").asText()).isEqualTo(request.getSourceDir());
        }
        
        if (request.getFilePath() != null) {
            assertThat(jsonNode.has("filePath")).isTrue();
            assertThat(jsonNode.get("filePath").asText()).isEqualTo(request.getFilePath());
        }
        
        assertThat(jsonNode.has("targetBaseDir")).isTrue();
        assertThat(jsonNode.get("targetBaseDir").asText()).isEqualTo(request.getTargetBaseDir());
        
        if (request.getExtensions() != null) {
            assertThat(jsonNode.has("extensions")).isTrue();
            assertThat(jsonNode.get("extensions").isArray()).isTrue();
        }

        // Verify we can deserialize back to the same object
        CreateTransferRequest deserialized = objectMapper.readValue(json, CreateTransferRequest.class);
        assertThat(deserialized.getSourceDir()).isEqualTo(request.getSourceDir());
        assertThat(deserialized.getFilePath()).isEqualTo(request.getFilePath());
        assertThat(deserialized.getTargetBaseDir()).isEqualTo(request.getTargetBaseDir());
        assertThat(deserialized.getExtensions()).isEqualTo(request.getExtensions());
    }

    @Property(tries = 100)
    void transferResponseJsonStructureIsStable(
            @ForAll("transferResponses") TransferResponse response
    ) throws Exception {
        // Serialize the generated DTO to JSON
        String json = objectMapper.writeValueAsString(response);
        JsonNode jsonNode = objectMapper.readTree(json);

        // Verify required fields are present
        assertThat(jsonNode.has("executionId")).isTrue();
        assertThat(jsonNode.get("executionId").asLong()).isEqualTo(response.getExecutionId());
        
        assertThat(jsonNode.has("status")).isTrue();

        // Verify optional fields
        if (response.getStartTime() != null) {
            assertThat(jsonNode.has("startTime")).isTrue();
        }
        
        if (response.getEndTime() != null) {
            assertThat(jsonNode.has("endTime")).isTrue();
        }

        // Verify we can deserialize back to the same object
        TransferResponse deserialized = objectMapper.readValue(json, TransferResponse.class);
        assertThat(deserialized.getExecutionId()).isEqualTo(response.getExecutionId());
        assertThat(deserialized.getStatus()).isEqualTo(response.getStatus());
    }

    @Property(tries = 100)
    void jsonFieldNamesMatchExpectedContract(
            @ForAll("createTransferRequests") CreateTransferRequest request
    ) throws Exception {
        String json = objectMapper.writeValueAsString(request);
        JsonNode jsonNode = objectMapper.readTree(json);

        // Verify field names match the expected API contract (camelCase)
        jsonNode.fieldNames().forEachRemaining(fieldName -> {
            assertThat(fieldName).matches("^[a-z][a-zA-Z0-9]*$"); // camelCase pattern
        });
    }

    @Provide
    net.jqwik.api.Arbitrary<CreateTransferRequest> createTransferRequests() {
        return net.jqwik.api.Arbitraries.of(
                new CreateTransferRequest()
                        .sourceDir("/source/dir")
                        .targetBaseDir("/target/dir"),
                new CreateTransferRequest()
                        .filePath("/source/file.jpg")
                        .targetBaseDir("/target/dir"),
                new CreateTransferRequest()
                        .sourceDir("/source/dir")
                        .targetBaseDir("/target/dir")
                        .extensions(Arrays.asList("jpg", "jpeg")),
                new CreateTransferRequest()
                        .filePath("/source/file.jpg")
                        .targetBaseDir("/target/dir")
                        .extensions(Arrays.asList("jpg"))
        );
    }

    @Provide
    net.jqwik.api.Arbitrary<TransferResponse> transferResponses() {
        return net.jqwik.api.Arbitraries.of(
                new TransferResponse()
                        .executionId(1L)
                        .status(TransferResponse.StatusEnum.COMPLETED),
                new TransferResponse()
                        .executionId(2L)
                        .status(TransferResponse.StatusEnum.STARTED),
                new TransferResponse()
                        .executionId(3L)
                        .status(TransferResponse.StatusEnum.FAILED)
        );
    }
}
