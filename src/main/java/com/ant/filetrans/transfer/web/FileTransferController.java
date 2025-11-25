package com.ant.filetrans.transfer.web;

import com.ant.filetrans.transfer.application.FileTransferService;
import com.ant.filetrans.transfer.application.TransferCommand;
import com.ant.filetrans.transfer.web.dto.CreateTransferRequest;
import com.ant.filetrans.transfer.web.mapper.TransferDtoMapper;
import com.ant.filetrans.transfer.web.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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

        TransferCommand command = TransferDtoMapper.toCommand(req);
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
        TransferResponse body = TransferDtoMapper.fromJobExecution(exec);

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

        return ResponseEntity.ok(TransferDtoMapper.fromJobExecution(exec));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
