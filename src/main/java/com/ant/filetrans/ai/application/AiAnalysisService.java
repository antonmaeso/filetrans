package com.ant.filetrans.ai.application;

import com.ant.filetrans.ai.api.AiAnalysisCompletedEvent;
import com.ant.filetrans.ai.api.AiMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;

    public void analyzeAndPublish(Path target) {
        log.info("Starting AI analysis for {}", target);
        AiMetadata metadata = aiClient.analyze(target);
        AiAnalysisCompletedEvent event = new AiAnalysisCompletedEvent(target, metadata);
        eventPublisher.publishEvent(event);
        log.info("Published AI analysis for {}", target);
    }
}
