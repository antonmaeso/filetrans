package com.ant.filetrans.metadata.application.extract;

import com.ant.filetrans.metadata.api.FileMetadata;
import com.ant.filetrans.metadata.api.MetadataKeys;
import lombok.extern.slf4j.Slf4j;

import com.drew.lang.Rational;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HexFormat;

@Slf4j
public class StandardMetadataExtractor implements MetadataExtractor {

    private static final HexFormat HEX = HexFormat.of();
    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        DECIMAL_FORMAT = new DecimalFormat("0.######", symbols);
        DECIMAL_FORMAT.setGroupingUsed(false);
    }

    private final List<MetadataContributor> contributors;

    public StandardMetadataExtractor(List<MetadataContributor> contributors) {
        this.contributors = contributors;
    }

    @Override
    public FileMetadata extract(Path file) {
        try {
            long size = Files.size(file);
            String contentType = Files.probeContentType(file);
            Instant lastModified = Files.getLastModifiedTime(file).toInstant();

            Map<String, Object> attributes = new HashMap<>();
            for (MetadataContributor contributor : contributors) {
                if (!contributor.supports(file)) {
                    continue;
                }
                try {
                    Map<String, Object> fragment = contributor.extractAttributes(file);
                    if (fragment != null) {
                        fragment.forEach((key, value) -> {
                            if (key != null && value != null) {
                                attributes.put(key, value);
                            }
                        });
                    }
                } catch (Exception e) {
                    log.warn("Metadata contributor {} failed for {}", contributor, file, e);
                }
            }

            String captureFingerprint = captureFingerprint(attributes);
            if (captureFingerprint != null) {
                attributes.put(MetadataKeys.CAPTURE_FINGERPRINT, captureFingerprint);
            }

            return new FileMetadata(file, size, contentType, lastModified, Map.copyOf(attributes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract metadata for file " + file, e);
        }
    }

    private static String captureFingerprint(Map<String, Object> attributes) {
        String[] parts = new String[]{
                asString(attributes.get(MetadataKeys.EXIF_MAKE)),
                asString(attributes.get(MetadataKeys.EXIF_MODEL)),
                asString(attributes.get(MetadataKeys.EXIF_CAMERA_SERIAL)),
                asString(attributes.get(MetadataKeys.EXIF_DATE_TIME_ORIGINAL)),
                asString(attributes.get(MetadataKeys.EXIF_SUBSECOND_ORIGINAL)),
                asString(attributes.get(MetadataKeys.EXIF_SHUTTER_SPEED)),
                asString(attributes.get(MetadataKeys.EXIF_APERTURE)),
                asString(attributes.get(MetadataKeys.EXIF_ISO)),
                asString(attributes.get(MetadataKeys.EXIF_FOCAL_LENGTH))
        };

        boolean hasValue = Arrays.stream(parts).anyMatch(part -> part != null && !part.isBlank());
        if (!hasValue) {
            return null;
        }

        String payload = String.join("|", Arrays.stream(parts)
                .map(part -> part == null ? "" : part.trim())
                .toList());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(payload.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest.digest());
        } catch (Exception e) {
            log.warn("Failed to compute capture fingerprint", e);
            return null;
        }
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Rational rational) {
            return rational.toSimpleString(true);
        }
        if (value instanceof Number number) {
            return DECIMAL_FORMAT.format(number.doubleValue());
        }
        return value.toString().trim();
    }
}
