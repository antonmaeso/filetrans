package com.ant.filetrans.transfer.web;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ant.filetrans.transfer.application.FileTransferService;

/**
 * Unit tests for FileTransferController using MockMvc.
 * Tests endpoint paths, HTTP methods, request validation, and response handling.
 */
@ExtendWith(MockitoExtension.class)
class FileTransferControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileTransferService fileTransferService;

    @InjectMocks
    private FileTransferController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn202WhenValidDirectoryTransferRequestProvided() throws Exception {
        // Given
        JobExecution mockExecution = createMockJobExecution(1L, BatchStatus.STARTED);
        when(fileTransferService.transferDirectory(any(), any(), any()))
                .thenReturn(mockExecution);

        String requestBody = """
                {
                    "sourceDir": "/source/path",
                    "targetBaseDir": "/target/path",
                    "extensions": ["jpg", "jpeg"]
                }
                """;

        // When & Then
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.executionId").value(1))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    void shouldReturn202WhenValidSingleFileTransferRequestProvided() throws Exception {
        // Given
        JobExecution mockExecution = createMockJobExecution(2L, BatchStatus.STARTED);
        when(fileTransferService.transferSingleFile(any(), any(), any()))
                .thenReturn(mockExecution);

        String requestBody = """
                {
                    "filePath": "/source/file.jpg",
                    "targetBaseDir": "/target/path"
                }
                """;

        // When & Then
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.executionId").value(2))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    void shouldReturn400WhenTargetBaseDirMissing() throws Exception {
        // Given
        String requestBody = """
                {
                    "sourceDir": "/source/path"
                }
                """;

        // When & Then
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenBothSourceDirAndFilePathMissing() throws Exception {
        // Given
        String requestBody = """
                {
                    "targetBaseDir": "/target/path"
                }
                """;

        // When & Then
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WhenExecutionIdFound() throws Exception {
        // Given
        Long executionId = 123L;
        JobExecution mockExecution = createMockJobExecution(executionId, BatchStatus.COMPLETED);
        when(fileTransferService.getJobExecution(eq(executionId)))
                .thenReturn(mockExecution);

        // When & Then
        mockMvc.perform(get("/transfers/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturn404WhenExecutionIdNotFound() throws Exception {
        // Given
        Long executionId = 999L;
        when(fileTransferService.getJobExecution(eq(executionId)))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/transfers/{executionId}", executionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldIncludeTimestampsInResponseWhenAvailable() throws Exception {
        // Given
        JobExecution mockExecution = createMockJobExecution(1L, BatchStatus.COMPLETED);
        mockExecution.setStartTime(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        mockExecution.setEndTime(LocalDateTime.of(2024, 1, 15, 10, 35, 0));
        
        when(fileTransferService.getJobExecution(eq(1L)))
                .thenReturn(mockExecution);

        // When & Then
        mockMvc.perform(get("/transfers/{executionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists());
    }

    /**
     * Helper method to create a mock JobExecution for testing.
     */
    private JobExecution createMockJobExecution(Long executionId, BatchStatus status) {
        JobInstance jobInstance = new JobInstance(1L, "transferJob");
        JobExecution execution = new JobExecution(executionId, jobInstance, new JobParameters());
        execution.setStatus(status);
        return execution;
    }
}
