package com.ant.filetrans.metadata.infrastructure.events;

import com.ant.filetrans.ai.api.AiAnalysisCompletedEvent;
import com.ant.filetrans.metadata.application.MetadataPersistenceService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AiMetadataEventListener {

    private final MetadataPersistenceService metadataPersistenceService;

    public AiMetadataEventListener(MetadataPersistenceService metadataPersistenceService) {
        this.metadataPersistenceService = metadataPersistenceService;
    }

    @EventListener
    public void on(AiAnalysisCompletedEvent event) {
        metadataPersistenceService.updateWithAiMetadata(event.target(), event.metadata());
    }
}
