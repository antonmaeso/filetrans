package com.ant.filetrans.ai.web.mapper;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.ant.filetrans.ai.api.model.AnalyzeAiResponse;

class AiApiMapperTest {

    @Test
    void convertsValidJpgPathToPath() {
        String pathString = "/path/to/image.jpg";

        Path result = AiApiMapper.toPath(pathString);

        assertEquals(Path.of(pathString), result);
    }

    @Test
    void convertsValidJpegPathToPath() {
        String pathString = "/path/to/image.jpeg";

        Path result = AiApiMapper.toPath(pathString);

        assertEquals(Path.of(pathString), result);
    }

    @Test
    void acceptsUppercaseJpgExtension() {
        String pathString = "/path/to/image.JPG";

        Path result = AiApiMapper.toPath(pathString);

        assertEquals(Path.of(pathString), result);
    }

    @Test
    void acceptsUppercaseJpegExtension() {
        String pathString = "/path/to/image.JPEG";

        Path result = AiApiMapper.toPath(pathString);

        assertEquals(Path.of(pathString), result);
    }

    @Test
    void acceptsMixedCaseJpgExtension() {
        String pathString = "/path/to/image.JpG";

        Path result = AiApiMapper.toPath(pathString);

        assertEquals(Path.of(pathString), result);
    }

    @Test
    void acceptsMixedCaseJpegExtension() {
        String pathString = "/path/to/image.JpEg";

        Path result = AiApiMapper.toPath(pathString);

        assertEquals(Path.of(pathString), result);
    }

    @Test
    void throwsExceptionWhenPathIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath(null)
        );

        assertTrue(exception.getMessage().contains("path must not be blank"));
    }

    @Test
    void throwsExceptionWhenPathIsEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath("")
        );

        assertTrue(exception.getMessage().contains("path must not be blank"));
    }

    @Test
    void throwsExceptionWhenPathIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath("   ")
        );

        assertTrue(exception.getMessage().contains("path must not be blank"));
    }

    @Test
    void throwsExceptionWhenPathDoesNotEndWithJpgOrJpeg() {
        String pathString = "/path/to/image.png";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath(pathString)
        );

        assertTrue(exception.getMessage().contains("path must end with .jpg or .jpeg"));
        assertTrue(exception.getMessage().contains(pathString));
    }

    @Test
    void throwsExceptionForRawFileExtension() {
        String pathString = "/path/to/image.raw";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath(pathString)
        );

        assertTrue(exception.getMessage().contains("path must end with .jpg or .jpeg"));
    }

    @Test
    void throwsExceptionForNefFileExtension() {
        String pathString = "/path/to/image.nef";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath(pathString)
        );

        assertTrue(exception.getMessage().contains("path must end with .jpg or .jpeg"));
    }

    @Test
    void throwsExceptionForTxtFileExtension() {
        String pathString = "/path/to/file.txt";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath(pathString)
        );

        assertTrue(exception.getMessage().contains("path must end with .jpg or .jpeg"));
    }

    @Test
    void throwsExceptionForNoExtension() {
        String pathString = "/path/to/image";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath(pathString)
        );

        assertTrue(exception.getMessage().contains("path must end with .jpg or .jpeg"));
    }

    @Test
    void createsResponseWithConfirmationMessage() {
        String pathString = "/path/to/image.jpg";

        AnalyzeAiResponse response = AiApiMapper.toResponse(pathString);

        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("AI analysis triggered for"));
        assertTrue(response.getMessage().contains(pathString));
    }

    @Test
    void createsResponseWithPathField() {
        String pathString = "/path/to/image.jpeg";

        AnalyzeAiResponse response = AiApiMapper.toResponse(pathString);

        assertNotNull(response);
        assertEquals(pathString, response.getPath());
    }

    @Test
    void responseIncludesFullPathInMessage() {
        String pathString = "/Users/anton/Pictures/Imported/2024/2024-01-15/DSC_2345.JPG";

        AnalyzeAiResponse response = AiApiMapper.toResponse(pathString);

        assertTrue(response.getMessage().contains(pathString));
        assertEquals(pathString, response.getPath());
    }
}
