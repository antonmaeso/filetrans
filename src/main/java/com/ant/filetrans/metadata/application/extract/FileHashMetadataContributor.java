package com.ant.filetrans.metadata.application.extract;

import com.ant.filetrans.metadata.api.MetadataKeys;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

public class FileHashMetadataContributor implements MetadataContributor {

    private static final HexFormat HEX = HexFormat.of();

    @Override
    public boolean supports(Path file) {
        return true;
    }

    @Override
    public Map<String, Object> extractAttributes(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return Map.of(MetadataKeys.FINGERPRINT_SHA256, HEX.formatHex(digest.digest()));
    }
}
