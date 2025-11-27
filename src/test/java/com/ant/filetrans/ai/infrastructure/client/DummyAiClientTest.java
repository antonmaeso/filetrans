package com.ant.filetrans.ai.infrastructure.client;

import com.ant.filetrans.ai.api.AiMetadata;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DummyAiClientTest {

    @Test
    void returnsDummyMetadata() {
        DummyAiClient client = new DummyAiClient();
        Path target = Path.of("/tmp/demo/image.jpg");

        AiMetadata metadata = client.analyze(target);

        assertThat(metadata.shortDescription()).contains("Dummy analysis for image.jpg");
        assertThat(metadata.tags()).contains("dummy", "auto-generated");
        assertThat(metadata.confidence()).isEqualTo(0.42);
    }
}
