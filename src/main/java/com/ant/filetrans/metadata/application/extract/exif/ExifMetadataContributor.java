package com.ant.filetrans.metadata.application.extract.exif;

import com.ant.filetrans.metadata.api.MetadataKeys;
import com.ant.filetrans.metadata.application.extract.MetadataContributor;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ExifMetadataContributor implements MetadataContributor {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".nef") || name.endsWith(".cr2");
    }

    @Override
    public Map<String, Object> extractAttributes(Path file) throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());

        for (ExifIFD0Directory directory : metadata.getDirectoriesOfType(ExifIFD0Directory.class)) {
            putIfPresent(attrs, MetadataKeys.EXIF_MAKE, directory::getString, ExifIFD0Directory.TAG_MAKE);
            putIfPresent(attrs, MetadataKeys.EXIF_MODEL, directory::getString, ExifIFD0Directory.TAG_MODEL);
        }

        Iterable<ExifSubIFDDirectory> subDirectories = metadata.getDirectoriesOfType(ExifSubIFDDirectory.class);
        for (ExifSubIFDDirectory exif : subDirectories) {
            putIfPresent(attrs, MetadataKeys.EXIF_LENS_MODEL, exif::getString, ExifSubIFDDirectory.TAG_LENS_MODEL);
            putIfPresent(attrs, MetadataKeys.EXIF_SHUTTER_SPEED, exif::getDescription, ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
            putIfPresent(attrs, MetadataKeys.EXIF_APERTURE, exif::getDoubleObject, ExifSubIFDDirectory.TAG_FNUMBER);
            putIfPresent(attrs, MetadataKeys.EXIF_ISO, exif::getInteger, ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
            putIfPresent(attrs, MetadataKeys.EXIF_FOCAL_LENGTH, exif::getDoubleObject, ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
            putIfPresent(attrs, MetadataKeys.EXIF_EXPOSURE_PROGRAM, exif::getDescription, ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM);
            putIfPresent(attrs, MetadataKeys.EXIF_DATE_TIME_ORIGINAL, exif::getString, ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            putIfPresent(attrs, MetadataKeys.EXIF_SUBSECOND_ORIGINAL, exif::getString, ExifSubIFDDirectory.TAG_SUBSECOND_TIME_ORIGINAL);
            putIfPresent(attrs, MetadataKeys.EXIF_CAMERA_SERIAL, exif::getString, ExifSubIFDDirectory.TAG_BODY_SERIAL_NUMBER);
        }

        ensureFromAnyDirectory(metadata, attrs, MetadataKeys.EXIF_DATE_TIME_ORIGINAL, Directory::getString, ExifDirectoryBase.TAG_DATETIME_ORIGINAL);
        ensureFromAnyDirectory(metadata, attrs, MetadataKeys.EXIF_SUBSECOND_ORIGINAL, Directory::getString, ExifDirectoryBase.TAG_SUBSECOND_TIME_ORIGINAL);
        ensureFromAnyDirectory(metadata, attrs, MetadataKeys.EXIF_CAMERA_SERIAL, Directory::getString, ExifDirectoryBase.TAG_BODY_SERIAL_NUMBER);
        ensureFromAnyDirectory(metadata, attrs, MetadataKeys.EXIF_SHUTTER_SPEED, Directory::getString, ExifDirectoryBase.TAG_EXPOSURE_TIME);
        ensureFromAnyDirectory(metadata, attrs, MetadataKeys.EXIF_APERTURE, Directory::getDoubleObject, ExifDirectoryBase.TAG_FNUMBER, ExifDirectoryBase.TAG_APERTURE);
        ensureFromAnyDirectory(metadata, attrs, MetadataKeys.EXIF_ISO, Directory::getInteger, ExifDirectoryBase.TAG_ISO_EQUIVALENT, ExifDirectoryBase.TAG_ISO_SPEED, ExifDirectoryBase.TAG_SENSITIVITY_TYPE);
        ensureFromAnyDirectory(metadata, attrs, MetadataKeys.EXIF_FOCAL_LENGTH, Directory::getDoubleObject, ExifDirectoryBase.TAG_FOCAL_LENGTH);
        ensureFromAnyDirectory(metadata, attrs, MetadataKeys.EXIF_LENS_MODEL, Directory::getString, ExifDirectoryBase.TAG_LENS_MODEL);

        GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gps != null && gps.getGeoLocation() != null) {
            var loc = gps.getGeoLocation();
            attrs.put(MetadataKeys.GPS_LATITUDE, loc.getLatitude());
            attrs.put(MetadataKeys.GPS_LONGITUDE, loc.getLongitude());
            attrs.put(MetadataKeys.GPS_ALTITUDE, gps.getDoubleObject(GpsDirectory.TAG_ALTITUDE));
        }

        return attrs;
    }

    private static <T> void ensureFromAnyDirectory(Metadata metadata,
                                                   Map<String, Object> attrs,
                                                   String key,
                                                   DirectoryTagExtractor<T> extractor,
                                                   int... tags) {
        if (attrs.containsKey(key)) {
            return;
        }
        for (Directory directory : metadata.getDirectories()) {
            for (int tag : tags) {
                if (!directory.containsTag(tag)) {
                    continue;
                }
                try {
                    T value = extractor.extract(directory, tag);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof String s && s.isBlank()) {
                        continue;
                    }
                    attrs.put(key, value);
                    return;
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static <T> void putIfPresent(Map<String, Object> attrs,
                                         String key,
                                         DirectoryValueSupplier<T> supplier,
                                         int tag) {
        if (attrs.containsKey(key)) {
            return;
        }
        T value = supplier.get(tag);
        if (value == null) {
            return;
        }
        if (value instanceof String s && s.isBlank()) {
            return;
        }
        attrs.put(key, value);
    }

    @FunctionalInterface
    private interface DirectoryValueSupplier<T> {
        T get(int tag);
    }

    @FunctionalInterface
    private interface DirectoryTagExtractor<T> {
        T extract(Directory directory, int tag);
    }
}
