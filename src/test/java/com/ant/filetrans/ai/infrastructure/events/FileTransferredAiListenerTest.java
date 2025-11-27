package com.ant.filetrans.ai.infrastructure.events;

import com.ant.filetrans.ai.application.AiAnalysisService;
import com.ant.filetrans.transfer.domain.FileTransferredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileTransferredAiListenerTest {

    @Mock
    private AiAnalysisService aiAnalysisService;

    @Test
    void triggersForJpg() {
        Path jpg = Path.of("/tmp/demo/photo.jpg");
        FileTransferredAiListener listener = new FileTransferredAiListener(aiAnalysisService);

        listener.on(new FileTransferredEvent(jpg));

        verify(aiAnalysisService).analyzeAndPublish(jpg);
    }

    @Test
    void skipsNonJpg() {
        Path png = Path.of("/tmp/demo/photo.png");
        FileTransferredAiListener listener = new FileTransferredAiListener(aiAnalysisService);

        listener.on(new FileTransferredEvent(png));

        verify(aiAnalysisService, never()).analyzeAndPublish(png);
    }
}
