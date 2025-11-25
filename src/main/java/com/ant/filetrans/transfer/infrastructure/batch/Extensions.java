package com.ant.filetrans.transfer.infrastructure.batch;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Normalized set of extensions used to filter files.
 */
public record Extensions(Set<String> values) {

    private static final Extensions EMPTY = new Extensions(Set.of());

    public static Extensions parse(String param) {
        if (param == null || param.isBlank()) {
            return EMPTY;
        }
        Set<String> normalized = Arrays.stream(param.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return EMPTY;
        }
        return new Extensions(Set.copyOf(normalized));
    }

    public static Extensions of(Set<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return EMPTY;
        }
        return new Extensions(extensions.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean accepts(Path path) {
        if (isEmpty()) {
            return true;
        }
        String ext = extensionOf(path);
        return !ext.isEmpty() && values.contains(ext);
    }

    public String asParameter() {
        return String.join(",", values);
    }

    private static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase();
    }
}
