package com.ant.filetrans.metadata;

import com.ant.filetrans.metadata.application.extract.BasicMetadataContributor;
import com.ant.filetrans.metadata.application.extract.FileHashMetadataContributor;
import com.ant.filetrans.metadata.application.extract.MetadataContributor;
import com.ant.filetrans.metadata.application.extract.exif.ExifMetadataContributor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetadataConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }

    @Bean
    public MetadataContributor basicMetadataContributor() {
        return new BasicMetadataContributor();
    }

    @Bean
    public MetadataContributor exifMetadataContributor() {
        return new ExifMetadataContributor();
    }

    @Bean
    public MetadataContributor fileHashMetadataContributor() {
        return new FileHashMetadataContributor();
    }
}
