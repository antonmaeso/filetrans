package com.ant.filetrans.transfer.web.dto;

import java.time.Instant;

/**
 * Response representing the transfer job.
 */
public record TransferResponse(
        Long executionId,
        String status,
        Instant startTime,
        Instant endTime
) {}