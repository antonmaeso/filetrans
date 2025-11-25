package com.ant.filetrans.transfer.infrastructure.batch;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExtensionsTest {

    @Test
    void parsesCommaSeparatedValues() {
        Extensions extensions = Extensions.parse("jpg, png ,NEF");

        assertIterableEquals(List.of("jpg", "png", "nef"), extensions.values());
        assertTrue(extensions.accepts(Path.of("a.JPG")));
        assertFalse(extensions.accepts(Path.of("b.txt")));
    }

    @Test
    void emptyInputYieldsEmptyExtensions() {
        assertTrue(Extensions.parse(null).isEmpty());
        assertTrue(Extensions.parse("").isEmpty());
    }

    @Test
    void copyOfProtectsInternalState() {
        Extensions extensions = Extensions.of(new java.util.HashSet<>(Set.of("JPG", "txt")));

        assertIterableEquals(List.of("jpg", "txt"), extensions.values());
        assertThrows(UnsupportedOperationException.class,
                () -> extensions.values().add("new"));
    }
}
