package com.ant.filetrans.transfer.domain;

import java.nio.file.Path;

/**
 * Emitted after a file has been successfully moved.
 */
public record FileTransferredEvent(Path target) {
}
