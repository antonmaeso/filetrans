package com.ant.filetrans.metadata.web.mapper;

import java.nio.file.Path;

import com.ant.filetrans.metadata.api.model.AnalyzeMetadataResponse;

/**
 * Mapper for converting between generated OpenAPI DTOs and internal domain models.
 */
public final class MetadataApiMapper {

    private MetadataApiMapper() {
    }

    /**
     * Converts a targetBaseDir string to a Path object.
     *
     * @param targetBaseDir the directory path string
     * @return the Path object
     * @throws IllegalArgumentException if targetBaseDir is blank
     */
    public static Path toPath(String targetBaseDir) {
        if (isBlank(targetBaseDir)) {
            throw new IllegalArgumentException("targetBaseDir must not be blank");
        }
        return Path.of(targetBaseDir);
    }

    /**
     * Creates an AnalyzeMetadataResponse DTO with a confirmation message.
     *
     * @param targetBaseDir the directory path being analyzed
     * @return the generated DTO for API response
     */
    public static AnalyzeMetadataResponse toResponse(String targetBaseDir) {
        AnalyzeMetadataResponse response = new AnalyzeMetadataResponse();
        response.setMessage("Metadata analysis triggered for directory: " + targetBaseDir);
        response.setTargetBaseDir(targetBaseDir);
        return response;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
