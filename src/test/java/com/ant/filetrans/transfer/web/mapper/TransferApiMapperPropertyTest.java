package com.ant.filetrans.transfer.web.mapper;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;

import com.ant.filetrans.transfer.api.model.CreateTransferRequest;
import com.ant.filetrans.transfer.api.model.TransferResponse;
import com.ant.filetrans.transfer.application.TransferCommand;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Positive;

/**
 * Property-based tests for TransferApiMapper using jqwik.
 * Each test runs with minimum 100 iterations to validate universal properties.
 */
class TransferApiMapperPropertyTest {

    /**
     * Property 4: Mapper Input Conversion
     * 
     * **Validates: Requirements 3.1, 3.4**
     * 
     * For any valid CreateTransferRequest DTO, the mapper SHALL successfully convert
     * it to the corresponding internal TransferCommand domain model with appropriate
     * data transformations (string to Path, list to Set, etc.).
     */
    @Property(tries = 100)
    void validCreateTransferRequestConvertsToTransferCommand(
            @ForAll("validCreateTransferRequest") CreateTransferRequest request) {
        
        // Act - should not throw exception
        TransferCommand command = assertDoesNotThrow(() -> TransferApiMapper.toCommand(request));
        
        // Assert - verify conversion succeeded and fields are properly mapped
        assertNotNull(command);
        assertNotNull(command.targetBaseDir());
        assertEquals(Path.of(request.getTargetBaseDir()), command.targetBaseDir());
        
        // Verify sourceDir or filePath is present (at least one must be non-null)
        assertTrue(command.sourceDir() != null || command.filePath() != null,
                "Either sourceDir or filePath must be present in command");
        
        // Verify sourceDir mapping if present
        if (request.getSourceDir() != null && !request.getSourceDir().isBlank()) {
            assertNotNull(command.sourceDir());
            assertEquals(Path.of(request.getSourceDir()), command.sourceDir());
        }
        
        // Verify filePath mapping if present
        if (request.getFilePath() != null && !request.getFilePath().isBlank()) {
            assertNotNull(command.filePath());
            assertEquals(Path.of(request.getFilePath()), command.filePath());
        }
        
        // Verify extensions are properly handled
        assertNotNull(command.extensions());
    }

    /**
     * Property 5: Mapper Output Conversion
     * 
     * **Validates: Requirements 3.2**
     * 
     * For any JobExecution internal domain model, the mapper SHALL successfully
     * convert it to the corresponding generated TransferResponse DTO.
     */
    @Property(tries = 100)
    void jobExecutionConvertsToTransferResponse(
            @ForAll("validJobExecution") JobExecution execution) {
        
        // Act - should not throw exception
        TransferResponse response = assertDoesNotThrow(() -> 
                TransferApiMapper.fromJobExecution(execution));
        
        // Assert - verify conversion succeeded and fields are properly mapped
        assertNotNull(response);
        assertNotNull(response.getExecutionId());
        assertEquals(execution.getId(), response.getExecutionId());
        
        assertNotNull(response.getStatus());
        
        // Verify timestamp conversions
        if (execution.getStartTime() != null) {
            assertNotNull(response.getStartTime());
        }
        
        if (execution.getEndTime() != null) {
            assertNotNull(response.getEndTime());
        }
    }

    /**
     * Property 6: Mapper Null Handling
     * 
     * **Validates: Requirements 3.3**
     * 
     * For any optional field in a generated DTO, the mapper SHALL correctly handle
     * null values according to the API contract without throwing exceptions.
     */
    @Property(tries = 100)
    void mapperHandlesNullOptionalFieldsWithoutException(
            @ForAll("createTransferRequestWithNulls") CreateTransferRequest request) {
        
        // Act - should not throw exception even with null optional fields
        TransferCommand command = assertDoesNotThrow(() -> TransferApiMapper.toCommand(request));
        
        // Assert - verify conversion succeeded
        assertNotNull(command);
        assertNotNull(command.targetBaseDir());
        assertNotNull(command.extensions());
        
        // Verify at least one of sourceDir or filePath is present
        assertTrue(command.sourceDir() != null || command.filePath() != null);
    }

    /**
     * Property 7: Mapper Error Reporting
     * 
     * **Validates: Requirements 3.6**
     * 
     * For any invalid data that causes mapping to fail, the mapper SHALL throw
     * an exception with a descriptive message identifying the problem.
     */
    @Property(tries = 100)
    void mapperThrowsDescriptiveExceptionForInvalidData(
            @ForAll("invalidCreateTransferRequest") CreateTransferRequest request) {
        
        // Act & Assert - should throw IllegalArgumentException with descriptive message
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TransferApiMapper.toCommand(request)
        );
        
        // Verify exception message is descriptive
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("sourceDir") || 
                   exception.getMessage().contains("filePath"),
                "Exception message should mention the problematic fields");
    }

    // ========== Arbitraries (Data Generators) ==========

    /**
     * Generates valid CreateTransferRequest instances with either sourceDir or filePath.
     */
    @Provide
    Arbitrary<CreateTransferRequest> validCreateTransferRequest() {
        return Arbitraries.oneOf(
                // Case 1: sourceDir provided
                validPathString().flatMap(sourceDir ->
                        validPathString().flatMap(targetBaseDir ->
                                optionalExtensionsList().map(extensions -> {
                                    CreateTransferRequest req = new CreateTransferRequest();
                                    req.setSourceDir(sourceDir);
                                    req.setTargetBaseDir(targetBaseDir);
                                    req.setExtensions(extensions);
                                    return req;
                                })
                        )
                ),
                // Case 2: filePath provided
                validPathString().flatMap(filePath ->
                        validPathString().flatMap(targetBaseDir ->
                                optionalExtensionsList().map(extensions -> {
                                    CreateTransferRequest req = new CreateTransferRequest();
                                    req.setFilePath(filePath);
                                    req.setTargetBaseDir(targetBaseDir);
                                    req.setExtensions(extensions);
                                    return req;
                                })
                        )
                ),
                // Case 3: both sourceDir and filePath provided
                validPathString().flatMap(sourceDir ->
                        validPathString().flatMap(filePath ->
                                validPathString().flatMap(targetBaseDir ->
                                        optionalExtensionsList().map(extensions -> {
                                            CreateTransferRequest req = new CreateTransferRequest();
                                            req.setSourceDir(sourceDir);
                                            req.setFilePath(filePath);
                                            req.setTargetBaseDir(targetBaseDir);
                                            req.setExtensions(extensions);
                                            return req;
                                        })
                                )
                        )
                )
        );
    }

    /**
     * Generates CreateTransferRequest instances with null optional fields.
     */
    @Provide
    Arbitrary<CreateTransferRequest> createTransferRequestWithNulls() {
        return Arbitraries.oneOf(
                // sourceDir provided, others null
                validPathString().map(targetBaseDir -> {
                    CreateTransferRequest req = new CreateTransferRequest();
                    req.setSourceDir("/some/source");
                    req.setTargetBaseDir(targetBaseDir);
                    req.setExtensions(null);
                    return req;
                }),
                // filePath provided, others null
                validPathString().map(targetBaseDir -> {
                    CreateTransferRequest req = new CreateTransferRequest();
                    req.setFilePath("/some/file.jpg");
                    req.setTargetBaseDir(targetBaseDir);
                    req.setExtensions(null);
                    return req;
                }),
                // sourceDir provided with empty extensions
                validPathString().map(targetBaseDir -> {
                    CreateTransferRequest req = new CreateTransferRequest();
                    req.setSourceDir("/some/source");
                    req.setTargetBaseDir(targetBaseDir);
                    req.setExtensions(new ArrayList<>());
                    return req;
                })
        );
    }

    /**
     * Generates invalid CreateTransferRequest instances (missing both sourceDir and filePath).
     */
    @Provide
    Arbitrary<CreateTransferRequest> invalidCreateTransferRequest() {
        return validPathString().map(targetBaseDir -> {
            CreateTransferRequest req = new CreateTransferRequest();
            req.setTargetBaseDir(targetBaseDir);
            // Both sourceDir and filePath are null/blank - this is invalid
            return req;
        }).injectDuplicates(0.1).edgeCases(config -> {
            // Add edge cases with blank strings
            config.add(createInvalidRequestWithBlanks("/target"));
        });
    }

    /**
     * Generates valid JobExecution instances with various states.
     */
    @Provide
    Arbitrary<JobExecution> validJobExecution() {
        return Arbitraries.longs().greaterOrEqual(1L).flatMap(executionId ->
                Arbitraries.of(BatchStatus.values()).flatMap(status ->
                        optionalLocalDateTime().flatMap(startTime ->
                                optionalLocalDateTime().map(endTime -> {
                                    JobInstance jobInstance = new JobInstance(1L, "testJob");
                                    JobExecution execution = new JobExecution(executionId, jobInstance, new JobParameters());
                                    execution.setStatus(status);
                                    if (startTime != null) {
                                        execution.setStartTime(startTime);
                                    }
                                    if (endTime != null) {
                                        execution.setEndTime(endTime);
                                    }
                                    return execution;
                                })
                        )
                )
        );
    }

    /**
     * Generates valid path strings (Unix-style paths).
     */
    private Arbitrary<String> validPathString() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('/', '-', '_', '.')
                .ofMinLength(1)
                .ofMaxLength(50)
                .filter(s -> !s.isBlank())
                .map(s -> s.startsWith("/") ? s : "/" + s);
    }

    /**
     * Generates optional extension lists.
     */
    private Arbitrary<List<String>> optionalExtensionsList() {
        return Arbitraries.oneOf(
                Arbitraries.just((List<String>) null),
                Arbitraries.just(new ArrayList<String>()),
                Arbitraries.of("jpg", "jpeg", "png", "raw", "nef", "cr2")
                        .list()
                        .ofMinSize(1)
                        .ofMaxSize(5)
        );
    }

    /**
     * Generates optional LocalDateTime instances.
     */
    private Arbitrary<LocalDateTime> optionalLocalDateTime() {
        return Arbitraries.oneOf(
                Arbitraries.just((LocalDateTime) null),
                Arbitraries.integers().between(2020, 2025).flatMap(year ->
                        Arbitraries.integers().between(1, 12).flatMap(month ->
                                Arbitraries.integers().between(1, 28).flatMap(day ->
                                        Arbitraries.integers().between(0, 23).flatMap(hour ->
                                                Arbitraries.integers().between(0, 59).flatMap(minute ->
                                                        Arbitraries.integers().between(0, 59).map(second ->
                                                                LocalDateTime.of(year, month, day, hour, minute, second)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Helper to create invalid request with blank strings.
     */
    private CreateTransferRequest createInvalidRequestWithBlanks(String targetBaseDir) {
        CreateTransferRequest req = new CreateTransferRequest();
        req.setSourceDir("   ");
        req.setFilePath("");
        req.setTargetBaseDir(targetBaseDir);
        return req;
    }
}
