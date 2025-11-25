package com.ant.filetrans.transfer.application;

import com.ant.filetrans.transfer.domain.FileTransferredEvent;
import com.ant.filetrans.transfer.domain.MovedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMoveService {

    private final FileMovePort fileMovePort;
    private final ApplicationEventPublisher events;

    public void executeMove(MovedFile movedFile) {
        fileMovePort.move(movedFile.source(), movedFile.target());
        events.publishEvent(new FileTransferredEvent(movedFile.target()));
        log.info("Moved {} -> {}", movedFile.source(), movedFile.target());
    }
}
