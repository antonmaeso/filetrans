package com.ant.filetrans.transfer.web.mapper;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.job.JobExecution;

import com.ant.filetrans.transfer.api.model.CreateTransferRequest;
import com.ant.filetrans.transfer.api.model.TransferResponse;
import com.ant.filetrans.transfer.application.TransferCommand;
import com.ant.filetrans.transfer.infrastructure.batch.Extensions;

/**
 * Mapper for converting between generated OpenAPI DTOs and internal domain models.
 */
public final class TransferApiMapper {

    private TransferApiMapper() {
    }

    /**
     * Converts a CreateTransferRequest DTO to a TransferCommand domain model.
     *
     * @param dto the generated DTO from OpenAPI specification
     * @return the internal domain command
     * @throws IllegalArgumentException if both sourceDir and filePath are missing
     */
    public static TransferCommand toCommand(CreateTransferRequest dto) {
        // Validate that either sourceDir or filePath is provided
        if (isBlank(dto.getSourceDir()) && isBlank(dto.getFilePath())) {
            throw new IllegalArgumentException(
                    "Invalid transfer request: either sourceDir or filePath must be provided");
        }

        Path targetBaseDir = toPath(dto.getTargetBaseDir());
        Path sourceDir = toPath(dto.getSourceDir());
        Path filePath = toPath(dto.getFilePath());
        Extensions extensions = Extensions.of(parseExtensions(dto.getExtensions()));

        return new TransferCommand(sourceDir, targetBaseDir, filePath, extensions);
    }

    /**
     * Converts a Spring Batch JobExecution to a TransferResponse DTO.
     *
     * @param execution the Spring Batch job execution
     * @return the generated DTO for API response
     */
    public static TransferResponse fromJobExecution(JobExecution execution) {
        TransferResponse response = new TransferResponse();
        
        response.setExecutionId(execution.getId());
        response.setStatus(mapStatus(execution.getStatus()));
        
        if (execution.getStartTime() != null) {
            OffsetDateTime startTime = execution.getStartTime()
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
            response.setStartTime(startTime);
        }
        
        if (execution.getEndTime() != null) {
            OffsetDateTime endTime = execution.getEndTime()
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
            response.setEndTime(endTime);
        }
        
        return response;
    }

    /**
     * Maps Spring Batch BatchStatus enum to the generated TransferResponse.StatusEnum.
     */
    private static TransferResponse.StatusEnum mapStatus(org.springframework.batch.core.BatchStatus batchStatus) {
        return switch (batchStatus) {
            case STARTING -> TransferResponse.StatusEnum.STARTING;
            case STARTED -> TransferResponse.StatusEnum.STARTED;
            case STOPPING -> TransferResponse.StatusEnum.STOPPING;
            case STOPPED -> TransferResponse.StatusEnum.STOPPED;
            case FAILED -> TransferResponse.StatusEnum.FAILED;
            case COMPLETED -> TransferResponse.StatusEnum.COMPLETED;
            case ABANDONED -> TransferResponse.StatusEnum.ABANDONED;
            default -> TransferResponse.StatusEnum.UNKNOWN;
        };
    }

    /**
     * Converts a string path to a Path object, returning null for blank strings.
     */
    private static Path toPath(String value) {
        if (isBlank(value)) {
            return null;
        }
        return Path.of(value);
    }

    /**
     * Parses and normalizes a list of file extensions.
     * Strips leading dots and wildcards, converts to lowercase, and removes blanks.
     */
    private static Set<String> parseExtensions(List<String> extensions) {
        if (extensions == null) {
            return Set.of();
        }
        
        Set<String> normalized = new LinkedHashSet<>();
        for (String ext : extensions) {
            if (ext != null) {
                String trimmed = ext.trim();
                String stripped = stripWildcardsAndDots(trimmed);
                String lower = stripped.toLowerCase();
                if (!lower.isBlank()) {
                    normalized.add(lower);
                }
            }
        }
        return normalized;
    }

    /**
     * Strips leading dots and wildcards from an extension string.
     */
    private static String stripWildcardsAndDots(String value) {
        String result = value;
        while (!result.isEmpty() && (result.charAt(0) == '.' || result.charAt(0) == '*')) {
            result = result.substring(1);
        }
        return result;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
