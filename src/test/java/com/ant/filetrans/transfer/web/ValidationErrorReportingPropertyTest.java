package com.ant.filetrans.transfer.web;

import static org.mockito.Mockito.mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ant.filetrans.transfer.application.FileTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for validation error reporting using jqwik.
 * Tests Property 19: Validation Error Reporting
 *
 * **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
 */
class ValidationErrorReportingPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileTransferService mockService = mock(FileTransferService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new FileTransferController(mockService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    /**
     * Property 19: Validation Error Reporting
     *
     * **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
     *
     * For any request that violates OpenAPI constraints (missing required field, invalid format, etc.),
     * the system SHALL return 400 Bad Request with an error message that identifies the field,
     * describes the violation, and explains the expected format.
     */
    @Property(tries = 100)
    void missingRequiredFieldReturnsDescriptiveError(
            @ForAll("requestWithMissingTargetBaseDir") String requestJson) throws Exception {

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.field").value("targetBaseDir"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Property 19: Validation Error Reporting (Missing Both Source Fields)
     *
     * **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
     *
     * For any request missing both sourceDir and filePath, the mapper SHALL throw
     * IllegalArgumentException with a descriptive message.
     */
    @Property(tries = 100)
    void missingBothSourceFieldsReturnsDescriptiveError(
            @ForAll("requestWithMissingBothSources") String requestJson) throws Exception {

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Property 19: Validation Error Reporting (Empty Required Field)
     *
     * **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
     *
     * For any request with empty required fields, the system SHALL return 400
     * with a message identifying the field and constraint violation.
     */
    @Property(tries = 100)
    void emptyRequiredFieldReturnsDescriptiveError(
            @ForAll("requestWithEmptyTargetBaseDir") String requestJson) throws Exception {

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========== Arbitraries ==========

    /**
     * Generates requests with missing targetBaseDir field.
     */
    @Provide
    Arbitrary<String> requestWithMissingTargetBaseDir() {
        return Arbitraries.of(
            "{\"sourceDir\":\"/source/path\"}",
            "{\"filePath\":\"/file/path.jpg\"}",
            "{\"sourceDir\":\"/source\",\"extensions\":[\"jpg\"]}",
            "{\"filePath\":\"/file.jpg\",\"extensions\":[\"jpg\",\"jpeg\"]}"
        );
    }

    /**
     * Generates requests with targetBaseDir but missing both sourceDir and filePath.
     */
    @Provide
    Arbitrary<String> requestWithMissingBothSources() {
        return Arbitraries.of(
            "{\"targetBaseDir\":\"/target\"}",
            "{\"targetBaseDir\":\"/target/path\",\"extensions\":[\"jpg\"]}",
            "{\"targetBaseDir\":\"/dest\",\"extensions\":[\"jpg\",\"jpeg\",\"raw\"]}"
        );
    }

    /**
     * Generates requests with empty targetBaseDir field.
     */
    @Provide
    Arbitrary<String> requestWithEmptyTargetBaseDir() {
        return Arbitraries.of(
            "{\"targetBaseDir\":\"\",\"sourceDir\":\"/source\"}",
            "{\"targetBaseDir\":\"\",\"filePath\":\"/file.jpg\"}",
            "{\"targetBaseDir\":\" \",\"sourceDir\":\"/source\"}",
            "{\"targetBaseDir\":\" \",\"filePath\":\"/file.jpg\"}"
        );
    }
}
