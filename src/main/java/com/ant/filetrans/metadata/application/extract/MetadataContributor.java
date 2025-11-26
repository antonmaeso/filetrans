package com.ant.filetrans.metadata.application.extract;

import java.nio.file.Path;
import java.util.Map;

public interface MetadataContributor {

    boolean supports(Path file);

    Map<String, Object> extractAttributes(Path file) throws Exception;
}
