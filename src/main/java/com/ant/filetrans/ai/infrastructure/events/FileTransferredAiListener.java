package com.ant.filetrans.ai.infrastructure.events;

import com.ant.filetrans.ai.application.AiAnalysisService;
import com.ant.filetrans.transfer.domain.FileTransferredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
public class FileTransferredAiListener {

    private final AiAnalysisService aiAnalysisService;

    public FileTransferredAiListener(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @EventListener
    public void on(FileTransferredEvent event) {
        Path target = event.target();
        if (!isJpg(target)) {
            log.debug("Skipping AI analysis for non-JPG {}", target);
            return;
        }
        log.info("Received FileTransferredEvent, triggering AI analysis for {}", target);
        aiAnalysisService.analyzeAndPublish(target);
    }

    private boolean isJpg(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg");
    }
}
