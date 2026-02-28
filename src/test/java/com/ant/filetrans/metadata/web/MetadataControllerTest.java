package com.ant.filetrans.metadata.web;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ant.filetrans.metadata.application.RecursiveMetadataBootstrapService;
import com.ant.filetrans.transfer.web.GlobalExceptionHandler;

/**
 * Unit tests for MetadataController using MockMvc.
 * Tests endpoint paths, HTTP methods, request validation, and response handling.
 */
@ExtendWith(MockitoExtension.class)
class MetadataControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RecursiveMetadataBootstrapService recursiveMetadataBootstrapService;

    @InjectMocks
    private MetadataController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn202WhenValidTargetBaseDirProvided() throws Exception {
        String targetBaseDir = "/Users/test/Pictures";

        mockMvc.perform(post("/api/metadata/analyze")
                        .param("targetBaseDir", targetBaseDir))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.targetBaseDir").value(targetBaseDir));

        verify(recursiveMetadataBootstrapService).initializeFromBaseDirectory(any(Path.class));
    }

    @Test
    void shouldReturn400WhenTargetBaseDirMissing() throws Exception {
        mockMvc.perform(post("/api/metadata/analyze"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenTargetBaseDirBlank() throws Exception {
        mockMvc.perform(post("/api/metadata/analyze")
                        .param("targetBaseDir", ""))
                .andExpect(status().isBadRequest());
    }
}
