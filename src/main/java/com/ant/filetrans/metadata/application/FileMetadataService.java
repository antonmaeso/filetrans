package com.ant.filetrans.metadata.application;

import com.ant.filetrans.metadata.api.FileMetadata;
import com.ant.filetrans.metadata.application.extract.MetadataContributor;
import com.ant.filetrans.metadata.application.extract.MetadataExtractor;
import com.ant.filetrans.metadata.application.extract.StandardMetadataExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final MetadataExtractor metadataExtractor;

    public FileMetadata capture(Path file) throws IOException {
        return metadataExtractor.extract(file);
    }

    @Configuration
    public static class MetadataExtractorConfig {

        @Bean
        public MetadataExtractor metadataExtractor(List<MetadataContributor> contributors) {
            return new StandardMetadataExtractor(contributors);
        }
    }
}
