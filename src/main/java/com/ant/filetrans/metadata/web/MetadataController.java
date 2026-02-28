package com.ant.filetrans.metadata.web;


import java.nio.file.Path;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ant.filetrans.metadata.api.model.AnalyzeMetadataResponse;
import com.ant.filetrans.metadata.application.RecursiveMetadataBootstrapService;
import com.ant.filetrans.metadata.web.mapper.MetadataApiMapper;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metadata")
@Validated
public class MetadataController {

    private final RecursiveMetadataBootstrapService recursiveMetadataBootstrapService;

    /**
     * Trigger metadata analysis for a directory.
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeMetadataResponse> triggerMetadataAnalysis(
            @RequestParam("targetBaseDir") @NotBlank String targetBaseDir
    ) {
        log.info("HTTP request to trigger metadata analysis for base dir: {}", targetBaseDir);
        
        Path path = MetadataApiMapper.toPath(targetBaseDir);
        recursiveMetadataBootstrapService.initializeFromBaseDirectory(path);
        
        AnalyzeMetadataResponse response = MetadataApiMapper.toResponse(targetBaseDir);
        return ResponseEntity.accepted().body(response);
    }

}
