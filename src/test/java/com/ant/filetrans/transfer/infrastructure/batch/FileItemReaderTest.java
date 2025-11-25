package com.ant.filetrans.transfer.infrastructure.batch;

import com.ant.filetrans.transfer.domain.FileDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileItemReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsFilesRespectingExtensions() throws Exception {
        Path jpg = createFile("a.jpg");
        Path nef = createFile("b.NEF");
        createFile("c.txt");

        FileItemReader reader = new FileItemReader(tempDir.toString(), Extensions.parse("jpg,nef"));
        reader.open(new ExecutionContext());
        try {
            FileDescriptor first = reader.read();
            FileDescriptor second = reader.read();
            FileDescriptor third = reader.read();

            assertNotNull(first);
            assertNotNull(second);
            assertEquals(jpg, first.path());
            assertEquals(nef, second.path());
            assertNull(third); // only jpg + nef allowed
        } finally {
            reader.close();
        }
    }

    @Test
    void resumesFromSavedIndex() throws Exception {
        Path first = createFile("a.jpg");
        Path second = createFile("b.jpg");

        ExecutionContext context = new ExecutionContext();

        FileItemReader reader = new FileItemReader(tempDir.toString(), Extensions.parse(null));
        reader.open(context);

        FileDescriptor firstDescriptor = reader.read();
        assertNotNull(firstDescriptor);
        assertEquals(first, firstDescriptor.path());
        reader.update(context); // simulate successful chunk
        reader.close();

        FileItemReader restarted = new FileItemReader(tempDir.toString(), Extensions.parse(null));
        restarted.open(context);
        try {
            FileDescriptor resumed = restarted.read();
            assertNotNull(resumed);
            assertEquals(second, resumed.path());
            assertNull(restarted.read());
        } finally {
            restarted.close();
        }
    }

    private Path createFile(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, "test");
    }
}
