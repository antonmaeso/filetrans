package com.ant.filetrans.metadata.infrastructure.events;

import com.ant.filetrans.metadata.application.MetadataWorkService;
import com.ant.filetrans.transfer.domain.FileTransferredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataEventListener {

    private final MetadataWorkService workService;

    @EventListener
    public void onFileTransferred(FileTransferredEvent event) {
        log.info("Queuing metadata work for {}", event.target());
        workService.enqueue(event.target());
    }
}
