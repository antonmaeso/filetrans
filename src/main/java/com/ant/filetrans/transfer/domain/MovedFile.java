package com.ant.filetrans.transfer.domain;

import java.nio.file.Path;

public record MovedFile(Path source, Path target) {}
