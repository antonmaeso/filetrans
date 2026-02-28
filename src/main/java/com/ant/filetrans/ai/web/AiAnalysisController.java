package com.ant.filetrans.ai.web;

import java.nio.file.Path;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ant.filetrans.ai.api.model.AnalyzeAiResponse;
import com.ant.filetrans.ai.application.AiAnalysisService;
import com.ant.filetrans.ai.web.mapper.AiApiMapper;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    /**
     * Trigger AI analysis for a specific image file.
     */
    @PostMapping("/ai/analyze")
    public ResponseEntity<AnalyzeAiResponse> analyze(@RequestParam("path") @NotBlank String path) {
        log.info("Manual AI analysis requested for {}", path);
        
        Path target = AiApiMapper.toPath(path);
        aiAnalysisService.analyzeAndPublish(target);
        
        AnalyzeAiResponse response = AiApiMapper.toResponse(path);
        return ResponseEntity.ok(response);
    }
}
