package com.ant.filetrans.metadata.api;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

public record FileMetadata(
        Path file,
        long size,
        String contentType,
        Instant lastModified,
        Map<String, Object> attributes
) {

    public Object get(String key) {
        return attributes != null ? attributes.get(key) : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return type.isInstance(value) ? (T) value : null;
    }
}
