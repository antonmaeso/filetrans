package com.ant.filetrans.metadata;

import com.ant.filetrans.metadata.application.MetadataWorkService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class MetadataTestConfiguration {

    @Bean
    public MetadataWorkService metadataWorkService() {
        return Mockito.mock(MetadataWorkService.class);
    }
}
