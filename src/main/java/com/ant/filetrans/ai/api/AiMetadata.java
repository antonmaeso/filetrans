package com.ant.filetrans.ai.api;

import java.util.List;

/**
 * Data produced by the AI analysis for a single image.
 */
public record AiMetadata(
        String shortDescription,
        List<String> tags,
        double confidence
) {
}
