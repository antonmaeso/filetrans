package com.ant.filetrans.ai.web;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ant.filetrans.ai.application.AiAnalysisService;
import com.ant.filetrans.transfer.web.GlobalExceptionHandler;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.BeforeTry;

/**
 * Property-based tests for AiAnalysisController.
 * Validates universal properties across many generated inputs.
 */
class AiAnalysisControllerPropertyTest {

    private MockMvc mockMvc;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @BeforeTry
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AiAnalysisController controller = new AiAnalysisController(aiAnalysisService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Property 13: AI Analysis Success
     * 
     * **Validates: Requirements 6.2**
     * 
     * For any valid path parameter pointing to a JPG or JPEG file, the POST /ai/analyze
     * endpoint SHALL return 200 OK or 202 Accepted.
     */
    @Property(tries = 100)
    void shouldReturn200ForAnyValidJpgOrJpegPath(@ForAll("validJpgPaths") String path) throws Exception {
        mockMvc.perform(post("/ai/analyze")
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(path));
    }

    @Provide
    Arbitrary<String> validJpgPaths() {
        Arbitrary<String> basePath = Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('/', '-', '_', '.')
                .ofMinLength(5)
                .ofMaxLength(50);
        
        Arbitrary<String> extension = Arbitraries.of(".jpg", ".jpeg", ".JPG", ".JPEG", ".Jpg", ".Jpeg");
        
        return basePath.flatMap(base -> 
            extension.map(ext -> base + ext)
        );
    }
}
