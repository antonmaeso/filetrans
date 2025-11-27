package com.ant.filetrans.ai.api;

import java.nio.file.Path;

/**
 * Emitted when AI analysis for a file has completed.
 */
public record AiAnalysisCompletedEvent(
        Path target,
        AiMetadata metadata
) {
}
