package com.ant.filetrans.transfer.web.dto;

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
