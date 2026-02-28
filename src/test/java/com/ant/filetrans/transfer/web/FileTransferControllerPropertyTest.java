package com.ant.filetrans.transfer.web;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ant.filetrans.transfer.application.FileTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Positive;

/**
 * Property-based tests for FileTransferController using jqwik.
 * Each test runs with minimum 100 iterations to validate universal properties.
 */
class FileTransferControllerPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileTransferService mockService = mock(FileTransferService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new FileTransferController(mockService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    /**
     * Property 8: Valid Request Success Response
     *
     * **Validates: Requirements 4.2**
     *
     * For any valid transfer request, the POST /transfers endpoint SHALL return
     * 202 Accepted status with a TransferResponse body containing a non-null executionId.
     */
    @Property(tries = 100)
    void validTransferRequestReturns202WithExecutionId(
            @ForAll("validTransferRequest") String requestJson,
            @ForAll @Positive long executionId) throws Exception {

        // Arrange
        JobExecution mockExecution = createMockJobExecution(executionId, BatchStatus.STARTED);
        when(mockService.transferDirectory(any(), any(), any())).thenReturn(mockExecution);
        when(mockService.transferSingleFile(any(), any(), any())).thenReturn(mockExecution);

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.status").exists());
    }

    /**
     * Property 9: Invalid Request Rejection
     *
     * **Validates: Requirements 4.3**
     *
     * For any transfer request missing required fields (targetBaseDir and both
     * sourceDir/filePath), the POST /transfers endpoint SHALL return 400 Bad Request.
     */
    @Property(tries = 100)
    void invalidTransferRequestReturns400(
            @ForAll("invalidTransferRequest") String requestJson) throws Exception {

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    /**
     * Property 10: Execution ID Lookup Success
     *
     * **Validates: Requirements 4.5**
     *
     * For any valid executionId that exists in the system, the GET /transfers/{executionId}
     * endpoint SHALL return 200 OK with a TransferResponse body.
     */
    @Property(tries = 100)
    void existingExecutionIdReturns200WithResponse(
            @ForAll @Positive long executionId,
            @ForAll("batchStatus") BatchStatus status) throws Exception {

        // Arrange
        JobExecution mockExecution = createMockJobExecution(executionId, status);
        when(mockService.getJobExecution(eq(executionId))).thenReturn(mockExecution);

        // Act & Assert
        mockMvc.perform(get("/transfers/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.status").exists());
    }

    /**
     * Property 11: Execution ID Lookup Failure
     *
     * **Validates: Requirements 4.6**
     *
     * For any executionId that does not exist in the system, the GET /transfers/{executionId}
     * endpoint SHALL return 404 Not Found.
     */
    @Property(tries = 100)
    void nonExistentExecutionIdReturns404(
            @ForAll @Positive long executionId) throws Exception {

        // Arrange
        when(mockService.getJobExecution(eq(executionId))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/transfers/{executionId}", executionId))
                .andExpect(status().isNotFound());
    }

    // ========== Arbitrary Providers ==========

    /**
     * Provides valid transfer request JSON with either sourceDir or filePath.
     */
    @Provide
    Arbitrary<String> validTransferRequest() {
        Arbitrary<String> sourceDir = Arbitraries.strings()
                .alpha().numeric().withChars('/', '_', '-', '.')
                .ofMinLength(5).ofMaxLength(50);

        Arbitrary<String> targetBaseDir = Arbitraries.strings()
                .alpha().numeric().withChars('/', '_', '-', '.')
                .ofMinLength(5).ofMaxLength(50);

        Arbitrary<List<String>> extensions = Arbitraries.of("jpg", "jpeg", "png", "raw", "nef")
                .list().ofMinSize(0).ofMaxSize(3);

        // Generate either directory transfer or single file transfer
        return Arbitraries.oneOf(
                // Directory transfer
                Combinators.combine(sourceDir, targetBaseDir, extensions)
                        .as((src, tgt, exts) -> {
                            try {
                                return objectMapper.writeValueAsString(new RequestBuilder()
                                        .sourceDir(src)
                                        .targetBaseDir(tgt)
                                        .extensions(exts)
                                        .build());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }),
                // Single file transfer
                Combinators.combine(sourceDir, targetBaseDir)
                        .as((file, tgt) -> {
                            try {
                                return objectMapper.writeValueAsString(new RequestBuilder()
                                        .filePath(file + "/file.jpg")
                                        .targetBaseDir(tgt)
                                        .build());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
        );
    }

    /**
     * Provides invalid transfer request JSON (missing required fields).
     */
    @Provide
    Arbitrary<String> invalidTransferRequest() {
        Arbitrary<String> path = Arbitraries.strings()
                .alpha().numeric().withChars('/', '_', '-', '.')
                .ofMinLength(5).ofMaxLength(50);

        return Arbitraries.oneOf(
                // Missing targetBaseDir
                path.map(src -> {
                    try {
                        return objectMapper.writeValueAsString(new RequestBuilder()
                                .sourceDir(src)
                                .build());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }),
                // Missing both sourceDir and filePath
                path.map(tgt -> {
                    try {
                        return objectMapper.writeValueAsString(new RequestBuilder()
                                .targetBaseDir(tgt)
                                .build());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }),
                // Null targetBaseDir
                Arbitraries.just("{}"),
                // Empty targetBaseDir
                path.map(src -> {
                    try {
                        return "{\"sourceDir\":\"" + src + "\",\"targetBaseDir\":\"\"}";
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    /**
     * Provides all possible BatchStatus enum values.
     */
    @Provide
    Arbitrary<BatchStatus> batchStatus() {
        return Arbitraries.of(
                BatchStatus.STARTING,
                BatchStatus.STARTED,
                BatchStatus.STOPPING,
                BatchStatus.STOPPED,
                BatchStatus.FAILED,
                BatchStatus.COMPLETED,
                BatchStatus.ABANDONED
        );
    }

    // ========== Helper Methods ==========

    private JobExecution createMockJobExecution(Long executionId, BatchStatus status) {
        JobInstance jobInstance = new JobInstance(1L, "transferJob");
        JobExecution execution = new JobExecution(executionId, jobInstance, new JobParameters());
        execution.setStatus(status);
        execution.setStartTime(LocalDateTime.now());
        return execution;
    }

    /**
     * Helper class to build request JSON objects.
     */
    private static class RequestBuilder {
        private String sourceDir;
        private String targetBaseDir;
        private String filePath;
        private List<String> extensions;

        RequestBuilder sourceDir(String sourceDir) {
            this.sourceDir = sourceDir;
            return this;
        }

        RequestBuilder targetBaseDir(String targetBaseDir) {
            this.targetBaseDir = targetBaseDir;
            return this;
        }

        RequestBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        RequestBuilder extensions(List<String> extensions) {
            this.extensions = extensions;
            return this;
        }

        java.util.Map<String, Object> build() {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            if (sourceDir != null) map.put("sourceDir", sourceDir);
            if (targetBaseDir != null) map.put("targetBaseDir", targetBaseDir);
            if (filePath != null) map.put("filePath", filePath);
            if (extensions != null && !extensions.isEmpty()) map.put("extensions", extensions);
            return map;
        }
    }
}
