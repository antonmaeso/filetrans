package com.ant.filetrans.metadata.web;


import com.ant.filetrans.metadata.application.RecursiveMetadataBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metadata")
public class MetadataController {

    private final RecursiveMetadataBootstrapService recursiveMetadataBootstrapService;

    /**
     * Trigger metadata analysis for a single file.
     */
    @PostMapping("/analyze")
    public ResponseEntity<String> triggerMetadataAnalysis(
            @RequestParam("targetBaseDir") String targetBaseDir
    ) {
        log.info("HTTP request to trigger metadata analysis for base dir: {}", targetBaseDir);
        recursiveMetadataBootstrapService.initializeFromBaseDirectory(Path.of(targetBaseDir));

        return ResponseEntity.accepted()
                .body("Metadata analysis triggered for base dir: " + targetBaseDir);
    }

}
