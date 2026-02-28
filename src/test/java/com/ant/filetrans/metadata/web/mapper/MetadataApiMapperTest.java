package com.ant.filetrans.metadata.web.mapper;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.ant.filetrans.metadata.api.model.AnalyzeMetadataResponse;

class MetadataApiMapperTest {

    @Test
    void convertsValidTargetBaseDirToPath() {
        String targetBaseDir = "/target/path";

        Path result = MetadataApiMapper.toPath(targetBaseDir);

        assertEquals(Path.of("/target/path"), result);
    }

    @Test
    void throwsExceptionWhenTargetBaseDirIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MetadataApiMapper.toPath(null)
        );

        assertTrue(exception.getMessage().contains("targetBaseDir must not be blank"));
    }

    @Test
    void throwsExceptionWhenTargetBaseDirIsEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MetadataApiMapper.toPath("")
        );

        assertTrue(exception.getMessage().contains("targetBaseDir must not be blank"));
    }

    @Test
    void throwsExceptionWhenTargetBaseDirIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MetadataApiMapper.toPath("   ")
        );

        assertTrue(exception.getMessage().contains("targetBaseDir must not be blank"));
    }

    @Test
    void createsResponseWithConfirmationMessage() {
        String targetBaseDir = "/target/path";

        AnalyzeMetadataResponse response = MetadataApiMapper.toResponse(targetBaseDir);

        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("Metadata analysis triggered"));
        assertTrue(response.getMessage().contains(targetBaseDir));
        assertEquals(targetBaseDir, response.getTargetBaseDir());
    }

    @Test
    void createsResponseWithCorrectTargetBaseDir() {
        String targetBaseDir = "/some/other/path";

        AnalyzeMetadataResponse response = MetadataApiMapper.toResponse(targetBaseDir);

        assertEquals(targetBaseDir, response.getTargetBaseDir());
    }
}
