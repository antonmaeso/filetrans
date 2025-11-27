package com.ant.filetrans.transfer.infrastructure.batch;

import com.ant.filetrans.transfer.application.FileMoveService;
import com.ant.filetrans.transfer.domain.MovedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

@Slf4j
@RequiredArgsConstructor
public class FileMoveItemWriter implements ItemWriter<MovedFile> {

    private final FileMoveService fileMoveService;
    private final DuplicateCatalogChecker duplicateCatalogChecker;

    @Override
    public void write(Chunk<? extends MovedFile> chunk) {
        log.info("Processing {} moved files", chunk.size());
        for (MovedFile movedFile : chunk.getItems()) {
            if (duplicateCatalogChecker.isDuplicate(movedFile)) {
                continue;
            }
            fileMoveService.executeMove(movedFile);
        }
    }
}
