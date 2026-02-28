package com.ant.filetrans.metadata.web;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ant.filetrans.metadata.application.RecursiveMetadataBootstrapService;
import com.ant.filetrans.transfer.web.GlobalExceptionHandler;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.lifecycle.BeforeTry;

/**
 * Property-based tests for MetadataController.
 * Validates universal properties across many generated inputs.
 */
class MetadataControllerPropertyTest {

    private MockMvc mockMvc;

    @Mock
    private RecursiveMetadataBootstrapService recursiveMetadataBootstrapService;

    @BeforeTry
    void setUp() {
        MockitoAnnotations.openMocks(this);
        MetadataController controller = new MetadataController(recursiveMetadataBootstrapService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Property 12: Metadata Analysis Success
     * 
     * **Validates: Requirements 5.2**
     * 
     * For any valid targetBaseDir parameter, the POST /api/metadata/analyze endpoint
     * SHALL return 202 Accepted with a response body.
     */
    @Property(tries = 100)
    void shouldReturn202ForAnyValidTargetBaseDir(@ForAll @NotBlank String targetBaseDir) throws Exception {
        mockMvc.perform(post("/api/metadata/analyze")
                        .param("targetBaseDir", targetBaseDir))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.targetBaseDir").value(targetBaseDir));
    }
}
