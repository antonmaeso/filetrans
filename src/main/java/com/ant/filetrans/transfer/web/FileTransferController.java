package com.ant.filetrans.transfer.web;

import java.net.URI;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ant.filetrans.transfer.api.model.CreateTransferRequest;
import com.ant.filetrans.transfer.api.model.TransferResponse;
import com.ant.filetrans.transfer.application.FileTransferService;
import com.ant.filetrans.transfer.application.TransferCommand;
import com.ant.filetrans.transfer.web.mapper.TransferApiMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
