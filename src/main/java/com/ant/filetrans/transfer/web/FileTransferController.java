package com.ant.filetrans.transfer.web;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ant.filetrans.transfer.api.model.CreateTransferRequest;
import com.ant.filetrans.transfer.api.model.TransferListResponse;
import com.ant.filetrans.transfer.api.model.TransferResponse;
import com.ant.filetrans.transfer.application.FileTransferService;
import com.ant.filetrans.transfer.application.TransferCommand;
import com.ant.filetrans.transfer.web.dto.PagedTransferListResponse;
import com.ant.filetrans.transfer.web.mapper.TransferApiMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * REST controller for file transfer jobs.
 *
 * Resource: /transfers
 * - POST /transfers                      → create a new transfer job (directory or single file)
 * - GET  /transfers                      → list all transfer jobs with pagination
 * - GET  /transfers/{executionId}        → get status of a transfer job
 * - DELETE /transfers/{executionId}      → delete a transfer job record
 * - POST /transfers/{executionId}/retry  → retry a failed transfer job
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
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request) throws Exception {
        log.info("Received transfer request: {}", request);

        TransferCommand command = TransferApiMapper.toCommand(request);
        JobExecution exec;

        // If filePath is provided → run single-file transfer
        if (command.isSingleFile()) {
            exec = fileTransferService.transferSingleFile(
                    command.filePath(),
                    command.targetBaseDir(),
                    command.extensions()
            );
        } else {
            // otherwise treat it as a directory transfer
            exec = fileTransferService.transferDirectory(
                    command.sourceDir(),
                    command.targetBaseDir(),
                    command.extensions()
            );
        }

        Long executionId = exec.getId();
        TransferResponse response = TransferApiMapper.fromJobExecution(exec);

        // Location: /transfers/{executionId}
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{executionId}")
                .buildAndExpand(executionId)
                .toUri();

        log.info("Job {} accepted", executionId);

        // 202 Accepted is idiomatic for async/batch work
        return ResponseEntity.accepted()
                .location(location)
                .body(response);
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

        TransferResponse response = TransferApiMapper.fromJobExecution(exec);
        return ResponseEntity.ok(response);
    }

    /**
     * List all transfer jobs with pagination.
     *
     * GET /transfers?page=0&size=20&sort=startTime,desc
     *
     * @param page zero-based page number (default: 0)
     * @param size number of items per page (default: 20)
     * @param sort sort parameter (currently ignored, always sorts by startTime desc)
     * @return paginated list of transfer jobs
     */
    @GetMapping
    public ResponseEntity<PagedTransferListResponse> listTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime,desc") String sort) {
        
        log.debug("Listing transfers: page={}, size={}, sort={}", page, size, sort);
        
        // Get paginated job executions
        List<JobExecution> executions = fileTransferService.getAllJobExecutions(page, size);
        long totalCount = fileTransferService.getTotalJobExecutionCount();
        
        // Map to DTOs
        List<TransferListResponse> content = executions.stream()
                .map(TransferApiMapper::toTransferListResponse)
                .collect(Collectors.toList());
        
        // Build paginated response
        PagedTransferListResponse response = PagedTransferListResponse.of(content, page, size, totalCount);
        
        log.debug("Returning {} transfers out of {} total", content.size(), totalCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a transfer job record.
     *
     * DELETE /transfers/{executionId}
     *
     * @param executionId the job execution ID to delete
     * @return 204 No Content on success, 409 Conflict if job is running
     */
    @DeleteMapping("/{executionId}")
    public ResponseEntity<Void> deleteTransfer(@PathVariable Long executionId) {
        log.info("Request to delete transfer job {}", executionId);
        
        JobExecution exec = fileTransferService.getJobExecution(executionId);
        if (exec == null) {
            log.warn("Transfer job {} not found", executionId);
            return ResponseEntity.notFound().build();
        }

        // Check if job is currently running
        if (exec.getStatus() == BatchStatus.STARTED || exec.getStatus() == BatchStatus.STARTING) {
            log.warn("Cannot delete running transfer job {}", executionId);
            return ResponseEntity.status(409).build(); // 409 Conflict
        }

        try {
            fileTransferService.deleteJobExecution(executionId);
            log.info("Deleted transfer job {}", executionId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete transfer job {}", executionId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Retry a failed transfer job.
     *
     * POST /transfers/{executionId}/retry
     *
     * @param executionId the job execution ID to retry
     * @return 201 Created with new execution details, 400 Bad Request if not failed
     */
    @PostMapping("/{executionId}/retry")
    public ResponseEntity<TransferResponse> retryTransfer(@PathVariable Long executionId) throws Exception {
        log.info("Request to retry transfer job {}", executionId);
        
        JobExecution originalExec = fileTransferService.getJobExecution(executionId);
        if (originalExec == null) {
            log.warn("Transfer job {} not found", executionId);
            return ResponseEntity.notFound().build();
        }

        // Check if job is in FAILED status
        if (originalExec.getStatus() != BatchStatus.FAILED) {
            log.warn("Cannot retry transfer job {} with status {}", executionId, originalExec.getStatus());
            return ResponseEntity.badRequest().build();
        }

        // Extract original parameters and retry
        JobExecution newExec = fileTransferService.retryJobExecution(executionId);
        
        Long newExecutionId = newExec.getId();
        TransferResponse response = TransferApiMapper.fromJobExecution(newExec);

        // Location: /transfers/{newExecutionId}
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/transfers/{executionId}")
                .buildAndExpand(newExecutionId)
                .toUri();

        log.info("Retried job {} as new execution {}", executionId, newExecutionId);

        return ResponseEntity.created(location).body(response);
    }
}
