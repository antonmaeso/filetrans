package com.ant.filetrans.ai.application;

import com.ant.filetrans.ai.api.AiAnalysisCompletedEvent;
import com.ant.filetrans.ai.api.AiMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<AiAnalysisCompletedEvent> eventCaptor;

    @Test
    void analyzesAndPublishesEvent() {
        Path target = Path.of("/tmp/photo.jpg");
        AiMetadata metadata = new AiMetadata("desc", List.of("a", "b"), 0.9);
        when(aiClient.analyze(target)).thenReturn(metadata);

        AiAnalysisService service = new AiAnalysisService(aiClient, eventPublisher);

        service.analyzeAndPublish(target);

        verify(aiClient).analyze(target);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AiAnalysisCompletedEvent published = eventCaptor.getValue();
        assertThat(published.target()).isEqualTo(target);
        assertThat(published.metadata()).isEqualTo(metadata);
    }
}
