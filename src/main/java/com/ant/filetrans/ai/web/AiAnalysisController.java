package com.ant.filetrans.ai.web;

import com.ant.filetrans.ai.application.AiAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@Slf4j
@RestController
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/ai/analyze")
    public void analyze(@RequestParam("path") String path) {
        Path target = Path.of(path);
        log.info("Manual AI analysis requested for {}", target);
        aiAnalysisService.analyzeAndPublish(target);
    }
}
