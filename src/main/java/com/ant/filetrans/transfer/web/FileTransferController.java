package com.ant.filetrans.transfer.web;

import com.ant.filetrans.transfer.application.FileTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;

/**
 * REST controller for file transfer jobs.
 *
 * Resource: /transfers
 * - POST /transfers              → create a new transfer job (directory or single file)
 * - GET  /transfers/{executionId} → get status of a transfer job
 */
@Slf4j
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class FileTransferController {

    private final FileTransferService fileTransferService;

    /**
     * Create a new transfer job.
     *
     * Directory transfer:
     *  POST /transfers
     *  {
     *    "sourceDir": "/Volumes/Camera/DCIM/101NZ7_2",
     *    "targetBaseDir": "/Users/anton/Pictures/Imported"
     *  }
     *
     * Single-file transfer:
     *  POST /transfers
     *  {
     *    "filePath": "/Volumes/Camera/DCIM/101NZ7_2/DSC_2345.JPG",
     *    "targetBaseDir": "/Users/anton/Pictures/Imported"
     *  }
     */
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(@RequestBody CreateTransferRequest req) throws Exception {
        log.info("Received transfer request: {}", req);

        if (isBlank(req.targetBaseDir())) {
            return ResponseEntity.badRequest().build();
        }
        if (isBlank(req.sourceDir()) && isBlank(req.filePath())) {
            return ResponseEntity.badRequest().build();
        }

        var extensions = normalizeExtensions(req.extensions());
        JobExecution exec;

        // If filePath is provided → run single-file transfer
        Path targetBaseDir = Path.of(req.targetBaseDir());
        if (!isBlank(req.filePath())) {
            exec = fileTransferService.transferSingleFile(
                    Path.of(req.filePath()),
                    targetBaseDir,
                    extensions
            );
        } else {
            // otherwise treat it as a directory transfer
            exec = fileTransferService.transferDirectory(
                    Path.of(req.sourceDir()),
                    targetBaseDir,
                    extensions
            );
        }

        Long executionId = exec.getId();
        TransferResponse body = toResponse(exec);

        // Location: /transfers/{executionId}
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{executionId}")
                .buildAndExpand(executionId)
                .toUri();

        log.info("Job {} accepted", executionId);

        // 202 Accepted is idiomatic for async/batch work
        return ResponseEntity.accepted()
                .location(location)
                .body(body);
    }

    /**
     * Get the status of a transfer job by its job execution id.
     *
     * GET /transfers/{executionId}
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable Long executionId) {
        log.debug("Fetching job execution {}", executionId);
        JobExecution exec = fileTransferService.getJobExecution(executionId);
        if (exec == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toResponse(exec));
    }

    /**
     * Request body for creating a transfer.
     *
     * - For directory transfer: set sourceDir + targetBaseDir
     * - For single-file transfer: set filePath + targetBaseDir
     */
    public record CreateTransferRequest(
            String sourceDir,
            String targetBaseDir,
            String filePath,
            java.util.List<String> extensions
    ) {}

    /**
     * Response representing the transfer job.
     */
    public record TransferResponse(
            Long executionId,
            String status,
            Instant startTime,
            Instant endTime
    ) {}

    private static TransferResponse toResponse(JobExecution exec) {
        Instant start = exec.getStartTime() != null
                ? exec.getStartTime().atZone(ZoneId.systemDefault()).toInstant()
                : null;
        Instant end = exec.getEndTime() != null
                ? exec.getEndTime().atZone(ZoneId.systemDefault()).toInstant()
                : null;

        return new TransferResponse(
                exec.getId(),                      // execution id, not job id
                exec.getStatus().toString(),
                start,
                end
        );
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static java.util.List<String> normalizeExtensions(java.util.List<String> extensions) {
        if (extensions == null) {
            return java.util.List.of();
        }
        return extensions.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(FileTransferController::stripWildcardsAndDots)
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    private static String stripWildcardsAndDots(String value) {
        String result = value;
        while (!result.isEmpty() && (result.charAt(0) == '.' || result.charAt(0) == '*')) {
            result = result.substring(1);
        }
        return result;
    }
}
