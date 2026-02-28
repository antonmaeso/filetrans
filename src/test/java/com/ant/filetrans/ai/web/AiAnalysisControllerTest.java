package com.ant.filetrans.ai.web;

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

import com.ant.filetrans.ai.application.AiAnalysisService;
import com.ant.filetrans.transfer.web.GlobalExceptionHandler;

/**
 * Unit tests for AiAnalysisController using MockMvc.
 * Tests endpoint paths, HTTP methods, request validation, and response handling.
 */
@ExtendWith(MockitoExtension.class)
class AiAnalysisControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @InjectMocks
    private AiAnalysisController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn200WhenValidJpgPathProvided() throws Exception {
        String path = "/Users/test/Pictures/image.jpg";

        mockMvc.perform(post("/ai/analyze")
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(path));

        verify(aiAnalysisService).analyzeAndPublish(any(Path.class));
    }

    @Test
    void shouldReturn200WhenValidJpegPathProvided() throws Exception {
        String path = "/Users/test/Pictures/image.JPEG";

        mockMvc.perform(post("/ai/analyze")
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(path));

        verify(aiAnalysisService).analyzeAndPublish(any(Path.class));
    }

    @Test
    void shouldReturn400WhenPathMissing() throws Exception {
        mockMvc.perform(post("/ai/analyze"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPathBlank() throws Exception {
        mockMvc.perform(post("/ai/analyze")
                        .param("path", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPathNotJpgOrJpeg() throws Exception {
        mockMvc.perform(post("/ai/analyze")
                        .param("path", "/Users/test/Pictures/image.png"))
                .andExpect(status().isBadRequest());
    }
}
