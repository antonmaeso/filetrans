package com.ant.filetrans.ai.infrastructure.client;

import com.ant.filetrans.ai.api.AiMetadata;
import com.ant.filetrans.ai.application.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class DummyAiClient implements AiClient {

    @Override
    public AiMetadata analyze(Path imagePath) {
        log.info("Dummy AI analyzing {}", imagePath);
        String filename = imagePath.getFileName().toString();
        String description = "Dummy analysis for " + filename;
        List<String> tags = List.of("dummy", "jpg", "auto-generated");
        double confidence = 0.42;
        log.info("Dummy AI produced description='{}', tags={}, confidence={}", description, tags, confidence);
        return new AiMetadata(description, tags, confidence);
    }
}
