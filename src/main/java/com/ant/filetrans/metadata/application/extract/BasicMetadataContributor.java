package com.ant.filetrans.metadata.application.extract;

import java.nio.file.Path;
import java.util.Map;

public class BasicMetadataContributor implements MetadataContributor {

    @Override
    public boolean supports(Path file) {
        return true;
    }

    @Override
    public Map<String, Object> extractAttributes(Path file) throws Exception {
        // nothing else for now; core metadata handled in StandardMetadataExtractor
        return Map.of();
    }
}
