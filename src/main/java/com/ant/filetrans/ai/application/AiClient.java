package com.ant.filetrans.ai.application;

import com.ant.filetrans.ai.api.AiMetadata;

import java.nio.file.Path;

public interface AiClient {
    AiMetadata analyze(Path imagePath);
}
