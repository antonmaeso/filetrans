package com.ant.filetrans.transfer.web.mapper;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;

import com.ant.filetrans.transfer.api.model.CreateTransferRequest;
import com.ant.filetrans.transfer.api.model.TransferResponse;
import com.ant.filetrans.transfer.application.TransferCommand;

class TransferApiMapperTest {

    @Test
    void convertsValidCreateTransferRequestWithSourceDirToTransferCommand() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setTargetBaseDir("/target/path");
        request.setExtensions(List.of("jpg", "png"));

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertEquals(Path.of("/source/path"), command.sourceDir());
        assertEquals(Path.of("/target/path"), command.targetBaseDir());
        assertNull(command.filePath());
        assertIterableEquals(List.of("jpg", "png"), command.extensions().values());
    }

    @Test
    void convertsValidCreateTransferRequestWithFilePathToTransferCommand() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setFilePath("/file/path.jpg");
        request.setTargetBaseDir("/target/path");

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertNull(command.sourceDir());
        assertEquals(Path.of("/target/path"), command.targetBaseDir());
        assertEquals(Path.of("/file/path.jpg"), command.filePath());
        assertTrue(command.extensions().isEmpty());
    }

    @Test
    void throwsExceptionWhenBothSourceDirAndFilePathAreMissing() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setTargetBaseDir("/target/path");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TransferApiMapper.toCommand(request)
        );

        assertTrue(exception.getMessage().contains("either sourceDir or filePath must be provided"));
    }

    @Test
    void throwsExceptionWhenBothSourceDirAndFilePathAreBlank() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("   ");
        request.setFilePath("");
        request.setTargetBaseDir("/target/path");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TransferApiMapper.toCommand(request)
        );

        assertTrue(exception.getMessage().contains("either sourceDir or filePath must be provided"));
    }

    @Test
    void handlesNullExtensions() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setTargetBaseDir("/target/path");
        request.setExtensions(null);

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertNotNull(command.extensions());
        assertTrue(command.extensions().isEmpty());
    }

    @Test
    void handlesNullSourceDir() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir(null);
        request.setFilePath("/file/path.jpg");
        request.setTargetBaseDir("/target/path");

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertNull(command.sourceDir());
        assertEquals(Path.of("/file/path.jpg"), command.filePath());
    }

    @Test
    void handlesNullFilePath() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setFilePath(null);
        request.setTargetBaseDir("/target/path");

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertEquals(Path.of("/source/path"), command.sourceDir());
        assertNull(command.filePath());
    }

    @Test
    void normalizesExtensionsByStrippingDots() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setTargetBaseDir("/target/path");
        request.setExtensions(List.of(".jpg", "..png", "nef"));

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertIterableEquals(List.of("jpg", "png", "nef"), command.extensions().values());
    }

    @Test
    void normalizesExtensionsByStrippingWildcards() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setTargetBaseDir("/target/path");
        request.setExtensions(List.of("*.jpg", "*png", "**nef"));

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertIterableEquals(List.of("jpg", "png", "nef"), command.extensions().values());
    }

    @Test
    void normalizesExtensionsByStrippingDotsAndWildcards() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setTargetBaseDir("/target/path");
        request.setExtensions(List.of("*.jpg", ".png", "*..nef"));

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertIterableEquals(List.of("jpg", "png", "nef"), command.extensions().values());
    }

    @Test
    void normalizesExtensionsByLowercasing() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setTargetBaseDir("/target/path");
        request.setExtensions(List.of("JPG", "Png", "NeF"));

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertIterableEquals(List.of("jpg", "png", "nef"), command.extensions().values());
    }

    @Test
    void normalizesExtensionsByTrimmingWhitespace() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setTargetBaseDir("/target/path");
        request.setExtensions(List.of("  jpg  ", " png", "nef "));

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertIterableEquals(List.of("jpg", "png", "nef"), command.extensions().values());
    }

    @Test
    void filtersOutBlankExtensions() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceDir("/source/path");
        request.setTargetBaseDir("/target/path");
        List<String> extensions = new java.util.ArrayList<>();
        extensions.add("jpg");
        extensions.add("   ");
        extensions.add("");
        extensions.add("png");
        extensions.add(null);
        request.setExtensions(extensions);

        TransferCommand command = TransferApiMapper.toCommand(request);

        assertIterableEquals(List.of("jpg", "png"), command.extensions().values());
    }

    @Test
    void convertsJobExecutionToTransferResponse() {
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution execution = new JobExecution(123L, jobInstance, new JobParameters());
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setStartTime(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        execution.setEndTime(LocalDateTime.of(2024, 1, 15, 11, 45, 30));

        TransferResponse response = TransferApiMapper.fromJobExecution(execution);

        assertEquals(123L, response.getExecutionId());
        assertEquals(TransferResponse.StatusEnum.COMPLETED, response.getStatus());
        assertNotNull(response.getStartTime());
        assertNotNull(response.getEndTime());
    }

    @Test
    void convertsTimestampFromLocalDateTimeToOffsetDateTime() {
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution execution = new JobExecution(456L, jobInstance, new JobParameters());
        execution.setStatus(BatchStatus.STARTED);
        LocalDateTime startTime = LocalDateTime.of(2024, 2, 20, 14, 15, 30);
        execution.setStartTime(startTime);

        TransferResponse response = TransferApiMapper.fromJobExecution(execution);

        OffsetDateTime expectedStartTime = startTime
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime();
        assertEquals(expectedStartTime, response.getStartTime());
    }

    @Test
    void handlesNullStartTimeInJobExecution() {
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution execution = new JobExecution(789L, jobInstance, new JobParameters());
        execution.setStatus(BatchStatus.STARTING);
        execution.setStartTime(null);

        TransferResponse response = TransferApiMapper.fromJobExecution(execution);

        assertEquals(789L, response.getExecutionId());
        assertEquals(TransferResponse.StatusEnum.STARTING, response.getStatus());
        assertNull(response.getStartTime());
    }

    @Test
    void handlesNullEndTimeInJobExecution() {
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution execution = new JobExecution(999L, jobInstance, new JobParameters());
        execution.setStatus(BatchStatus.STARTED);
        execution.setStartTime(LocalDateTime.of(2024, 3, 10, 9, 0, 0));
        execution.setEndTime(null);

        TransferResponse response = TransferApiMapper.fromJobExecution(execution);

        assertEquals(999L, response.getExecutionId());
        assertEquals(TransferResponse.StatusEnum.STARTED, response.getStatus());
        assertNotNull(response.getStartTime());
        assertNull(response.getEndTime());
    }

    @Test
    void mapsAllBatchStatusEnums() {
        assertStatusMapping(BatchStatus.STARTING, TransferResponse.StatusEnum.STARTING);
        assertStatusMapping(BatchStatus.STARTED, TransferResponse.StatusEnum.STARTED);
        assertStatusMapping(BatchStatus.STOPPING, TransferResponse.StatusEnum.STOPPING);
        assertStatusMapping(BatchStatus.STOPPED, TransferResponse.StatusEnum.STOPPED);
        assertStatusMapping(BatchStatus.FAILED, TransferResponse.StatusEnum.FAILED);
        assertStatusMapping(BatchStatus.COMPLETED, TransferResponse.StatusEnum.COMPLETED);
        assertStatusMapping(BatchStatus.ABANDONED, TransferResponse.StatusEnum.ABANDONED);
    }

    private void assertStatusMapping(BatchStatus batchStatus, TransferResponse.StatusEnum expectedStatus) {
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution execution = new JobExecution(1L, jobInstance, new JobParameters());
        execution.setStatus(batchStatus);

        TransferResponse response = TransferApiMapper.fromJobExecution(execution);

        assertEquals(expectedStatus, response.getStatus());
    }
}
