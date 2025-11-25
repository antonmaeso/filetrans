package com.ant.filetrans.transfer.application;

import com.ant.filetrans.transfer.infrastructure.batch.Extensions;

import java.nio.file.Path;

public record TransferCommand(
        Path sourceDir,
        Path targetBaseDir,
        Path filePath,
        Extensions extensions
) {
    public boolean isSingleFile() {
        return filePath != null;
    }
}
