package com.ant.filetrans.transfer.application;

import java.nio.file.Path;

/**
 * Port responsible for moving files in storage.
 */
public interface FileMovePort {
    void move(Path source, Path target);
}
