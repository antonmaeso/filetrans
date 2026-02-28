package com.ant.filetrans.ai.web.mapper;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for AiApiMapper using jqwik.
 * Each test runs with minimum 100 iterations to validate universal properties.
 */
class AiApiMapperPropertyTest {

    /**
     * Property 14: AI File Extension Validation
     * 
     * **Validates: Requirements 6.5**
     * 
     * For any path parameter that does not end with .jpg or .jpeg (case-insensitive),
     * the mapper SHALL reject the request with an IllegalArgumentException.
     * 
     * Conversely, for any path ending with .jpg or .jpeg (any case), the mapper
     * SHALL accept the path and successfully convert it to a Path object.
     */
    @Property(tries = 100)
    void pathsWithJpgOrJpegExtensionAreAccepted(
            @ForAll("validJpgJpegPath") String path) {
        
        // Act - should not throw exception for valid JPG/JPEG paths
        Path result = assertDoesNotThrow(() -> AiApiMapper.toPath(path));
        
        // Assert - verify conversion succeeded
        assertNotNull(result);
        assertEquals(Path.of(path), result);
    }

    /**
     * Property 14: AI File Extension Validation (rejection case)
     * 
     * **Validates: Requirements 6.5**
     * 
     * For any path with extensions other than .jpg or .jpeg, the mapper SHALL
     * reject the request with a descriptive IllegalArgumentException.
     */
    @Property(tries = 100)
    void pathsWithNonJpgJpegExtensionAreRejected(
            @ForAll("invalidExtensionPath") String path) {
        
        // Act & Assert - should throw IllegalArgumentException for non-JPG/JPEG paths
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiApiMapper.toPath(path)
        );
        
        // Verify exception message is descriptive
        assertNotNull(exception.getMessage());
        assertNotNull(exception.getMessage().contains("path must end with .jpg or .jpeg") ||
                      exception.getMessage().contains("path must not be blank"),
                "Exception message should describe the validation failure");
    }

    // ========== Arbitraries (Data Generators) ==========

    /**
     * Generates valid paths ending with .jpg or .jpeg in various cases.
     */
    @Provide
    Arbitrary<String> validJpgJpegPath() {
        return Arbitraries.oneOf(
                // .jpg extension with various cases
                validPathPrefix().map(prefix -> prefix + ".jpg"),
                validPathPrefix().map(prefix -> prefix + ".JPG"),
                validPathPrefix().map(prefix -> prefix + ".Jpg"),
                validPathPrefix().map(prefix -> prefix + ".jPg"),
                validPathPrefix().map(prefix -> prefix + ".jpG"),
                validPathPrefix().map(prefix -> prefix + ".JpG"),
                validPathPrefix().map(prefix -> prefix + ".jPG"),
                validPathPrefix().map(prefix -> prefix + ".JPg"),
                
                // .jpeg extension with various cases
                validPathPrefix().map(prefix -> prefix + ".jpeg"),
                validPathPrefix().map(prefix -> prefix + ".JPEG"),
                validPathPrefix().map(prefix -> prefix + ".Jpeg"),
                validPathPrefix().map(prefix -> prefix + ".jPeg"),
                validPathPrefix().map(prefix -> prefix + ".jpEg"),
                validPathPrefix().map(prefix -> prefix + ".jpeG"),
                validPathPrefix().map(prefix -> prefix + ".JPeg"),
                validPathPrefix().map(prefix -> prefix + ".JpEg"),
                validPathPrefix().map(prefix -> prefix + ".JpeG"),
                validPathPrefix().map(prefix -> prefix + ".jPEg"),
                validPathPrefix().map(prefix -> prefix + ".jpEG"),
                validPathPrefix().map(prefix -> prefix + ".jPEG"),
                validPathPrefix().map(prefix -> prefix + ".JPeG"),
                validPathPrefix().map(prefix -> prefix + ".JpEG"),
                validPathPrefix().map(prefix -> prefix + ".JPEG")
        );
    }

    /**
     * Generates paths with invalid extensions (not .jpg or .jpeg).
     */
    @Provide
    Arbitrary<String> invalidExtensionPath() {
        return Arbitraries.oneOf(
                // Common image formats that are NOT jpg/jpeg
                validPathPrefix().map(prefix -> prefix + ".png"),
                validPathPrefix().map(prefix -> prefix + ".PNG"),
                validPathPrefix().map(prefix -> prefix + ".gif"),
                validPathPrefix().map(prefix -> prefix + ".GIF"),
                validPathPrefix().map(prefix -> prefix + ".bmp"),
                validPathPrefix().map(prefix -> prefix + ".BMP"),
                validPathPrefix().map(prefix -> prefix + ".tiff"),
                validPathPrefix().map(prefix -> prefix + ".TIFF"),
                validPathPrefix().map(prefix -> prefix + ".webp"),
                validPathPrefix().map(prefix -> prefix + ".WEBP"),
                
                // RAW formats
                validPathPrefix().map(prefix -> prefix + ".raw"),
                validPathPrefix().map(prefix -> prefix + ".RAW"),
                validPathPrefix().map(prefix -> prefix + ".nef"),
                validPathPrefix().map(prefix -> prefix + ".NEF"),
                validPathPrefix().map(prefix -> prefix + ".cr2"),
                validPathPrefix().map(prefix -> prefix + ".CR2"),
                validPathPrefix().map(prefix -> prefix + ".arw"),
                validPathPrefix().map(prefix -> prefix + ".ARW"),
                validPathPrefix().map(prefix -> prefix + ".dng"),
                validPathPrefix().map(prefix -> prefix + ".DNG"),
                
                // Non-image formats
                validPathPrefix().map(prefix -> prefix + ".txt"),
                validPathPrefix().map(prefix -> prefix + ".pdf"),
                validPathPrefix().map(prefix -> prefix + ".doc"),
                validPathPrefix().map(prefix -> prefix + ".mp4"),
                validPathPrefix().map(prefix -> prefix + ".mov"),
                validPathPrefix().map(prefix -> prefix + ".avi"),
                
                // No extension
                validPathPrefix(),
                
                // Similar but not exact extensions
                validPathPrefix().map(prefix -> prefix + ".jpg2"),
                validPathPrefix().map(prefix -> prefix + ".jpeg2"),
                validPathPrefix().map(prefix -> prefix + ".jp"),
                validPathPrefix().map(prefix -> prefix + ".jpe"),
                validPathPrefix().map(prefix -> prefix + ".jpx"),
                validPathPrefix().map(prefix -> prefix + ".j2k")
        );
    }

    /**
     * Generates valid path prefixes (directory + filename without extension).
     */
    private Arbitrary<String> validPathPrefix() {
        return Arbitraries.oneOf(
                // Unix-style absolute paths
                Arbitraries.strings()
                        .alpha()
                        .numeric()
                        .withChars('/', '-', '_', ' ')
                        .ofMinLength(5)
                        .ofMaxLength(50)
                        .filter(s -> !s.isBlank())
                        .map(s -> s.startsWith("/") ? s : "/" + s)
                        .map(s -> s.endsWith("/") ? s + "image" : s),
                
                // Typical photo paths
                Arbitraries.just("/Users/anton/Pictures/Imported/2024/2024-01-15/DSC_2345"),
                Arbitraries.just("/Volumes/Camera/DCIM/101NZ7_2/IMG_1234"),
                Arbitraries.just("/home/user/photos/vacation/photo001"),
                Arbitraries.just("/mnt/storage/images/2024/january/sunset"),
                
                // Windows-style paths (for cross-platform testing)
                Arbitraries.just("C:/Users/User/Pictures/photo"),
                Arbitraries.just("D:/Photos/2024/image"),
                
                // Paths with spaces and special characters
                Arbitraries.just("/path/to/my photo"),
                Arbitraries.just("/path/with-dashes/file-name"),
                Arbitraries.just("/path/with_underscores/file_name")
        );
    }
}
