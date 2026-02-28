package com.ant.filetrans.ai.web.mapper;

import java.nio.file.Path;

import com.ant.filetrans.ai.api.model.AnalyzeAiResponse;

/**
 * Mapper for converting between generated OpenAPI DTOs and internal domain models.
 */
public final class AiApiMapper {

    private AiApiMapper() {
    }

    /**
     * Converts a path string to a Path object with JPG/JPEG validation.
     *
     * @param path the image file path string
     * @return the Path object
     * @throws IllegalArgumentException if path is blank or doesn't end with .jpg/.jpeg
     */
    public static Path toPath(String path) {
        if (isBlank(path)) {
            throw new IllegalArgumentException("path must not be blank");
        }
        
        String lowerPath = path.toLowerCase();
        if (!lowerPath.endsWith(".jpg") && !lowerPath.endsWith(".jpeg")) {
            throw new IllegalArgumentException(
                    "path must end with .jpg or .jpeg (case-insensitive), but was: " + path);
        }
        
        return Path.of(path);
    }

    /**
     * Creates an AnalyzeAiResponse DTO with a confirmation message.
     *
     * @param path the image path being analyzed
     * @return the generated DTO for API response
     */
    public static AnalyzeAiResponse toResponse(String path) {
        AnalyzeAiResponse response = new AnalyzeAiResponse();
        response.setMessage("AI analysis triggered for: " + path);
        response.setPath(path);
        return response;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
