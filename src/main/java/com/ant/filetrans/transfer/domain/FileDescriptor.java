package com.ant.filetrans.transfer.domain;

import java.nio.file.Path;
import java.time.Instant;

public record FileDescriptor(Path path, Instant lastModified) { }