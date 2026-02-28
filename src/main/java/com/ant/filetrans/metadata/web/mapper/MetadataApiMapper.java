package com.ant.filetrans.metadata.web.mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ant.filetrans.metadata.api.FileMetadata;
import com.ant.filetrans.metadata.api.MetadataKeys;
import com.ant.filetrans.metadata.api.model.AiAnalysisData;
import com.ant.filetrans.metadata.api.model.AnalyzeMetadataResponse;
import com.ant.filetrans.metadata.api.model.ExifData;
import com.ant.filetrans.metadata.api.model.FileListResponse;
import com.ant.filetrans.metadata.api.model.FileMetadataResponse;
import com.ant.filetrans.metadata.application.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mapper for converting between generated OpenAPI DTOs and internal domain models.
 */
public final class MetadataApiMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MetadataApiMapper() {
    }

    /**
     * Converts a targetBaseDir string to a Path object.
     *
     * @param targetBaseDir the directory path string
     * @return the Path object
     * @throws IllegalArgumentException if targetBaseDir is blank
     */
    public static Path toPath(String targetBaseDir) {
        if (isBlank(targetBaseDir)) {
            throw new IllegalArgumentException("targetBaseDir must not be blank");
        }
        return Path.of(targetBaseDir);
    }

    /**
     * Creates an AnalyzeMetadataResponse DTO with a confirmation message.
     *
     * @param targetBaseDir the directory path being analyzed
     * @return the generated DTO for API response
     */
    public static AnalyzeMetadataResponse toResponse(String targetBaseDir) {
        AnalyzeMetadataResponse response = new AnalyzeMetadataResponse();
        response.setMessage("Metadata analysis triggered for directory: " + targetBaseDir);
        response.setTargetBaseDir(targetBaseDir);
        return response;
    }

    /**
     * Converts FileInfo to FileListResponse DTO.
     *
     * @param fileInfo the file information
     * @return the DTO for API response
     */
    public static FileListResponse toFileListResponse(FileService.FileInfo fileInfo) {
        FileListResponse response = new FileListResponse();
        response.setId(fileInfo.id());
        response.setPath(fileInfo.path());
        response.setFilename(fileInfo.filename());
        response.setSize(fileInfo.size());
        response.setTransferDate(OffsetDateTime.ofInstant(fileInfo.transferDate(), ZoneId.systemDefault()));
        
        if (fileInfo.isImage()) {
            response.setThumbnailUrl("/files/" + fileInfo.id() + "/thumbnail");
        }
        
        return response;
    }

    /**
     * Converts file path and metadata to FileMetadataResponse DTO.
     *
     * @param file the file path
     * @param id the file identifier
     * @return the DTO for API response
     * @throws IOException if metadata cannot be read
     */
    public static FileMetadataResponse toFileMetadataResponse(Path file, String id) throws IOException {
        FileMetadataResponse response = new FileMetadataResponse();
        response.setId(id);
        response.setPath(file.toString());
        response.setFilename(file.getFileName().toString());
        response.setSize(Files.size(file));
        response.setTransferDate(OffsetDateTime.ofInstant(
            Files.getLastModifiedTime(file).toInstant(), 
            ZoneId.systemDefault()
        ));

        // Read metadata sidecar if it exists
        Path sidecarPath = file.resolveSibling(file.getFileName() + ".metadata.json");
        if (Files.exists(sidecarPath)) {
            FileMetadata metadata = objectMapper.readValue(sidecarPath.toFile(), FileMetadata.class);
            
            if (metadata.attributes() != null) {
                Map<String, Object> attrs = metadata.attributes();
                
                // Set hash information
                Object hash = attrs.get(MetadataKeys.FINGERPRINT_SHA256);
                if (hash != null) {
                    response.setHash(hash.toString());
                    response.setHashAlgorithm("SHA-256");
                }
                
                // Set source path if available
                Object sourcePath = attrs.get("sourcePath");
                if (sourcePath != null) {
                    response.setSourcePath(sourcePath.toString());
                }
                
                // Map EXIF data
                ExifData exifData = mapExifData(attrs);
                if (exifData != null) {
                    response.setExif(exifData);
                }
                
                // Map AI analysis data
                AiAnalysisData aiData = mapAiAnalysisData(attrs);
                if (aiData != null) {
                    response.setAiAnalysis(aiData);
                }
            }
        }

        return response;
    }

    private static ExifData mapExifData(Map<String, Object> attrs) {
        ExifData exif = new ExifData();
        boolean hasData = false;
        
        Object cameraModel = attrs.get(MetadataKeys.EXIF_MODEL);
        if (cameraModel != null) {
            exif.setCameraModel(cameraModel.toString());
            hasData = true;
        }
        
        Object captureDate = attrs.get(MetadataKeys.EXIF_DATE_TIME_ORIGINAL);
        if (captureDate != null) {
            exif.setCaptureDate(captureDate.toString());
            hasData = true;
        }
        
        Object gpsLat = attrs.get(MetadataKeys.GPS_LATITUDE);
        if (gpsLat != null) {
            try {
                exif.setGpsLatitude(Double.parseDouble(gpsLat.toString()));
                hasData = true;
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        Object gpsLon = attrs.get(MetadataKeys.GPS_LONGITUDE);
        if (gpsLon != null) {
            try {
                exif.setGpsLongitude(Double.parseDouble(gpsLon.toString()));
                hasData = true;
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        Object focalLength = attrs.get(MetadataKeys.EXIF_FOCAL_LENGTH);
        if (focalLength != null) {
            exif.setFocalLength(focalLength.toString());
            hasData = true;
        }
        
        Object aperture = attrs.get(MetadataKeys.EXIF_APERTURE);
        if (aperture != null) {
            exif.setAperture(aperture.toString());
            hasData = true;
        }
        
        Object iso = attrs.get(MetadataKeys.EXIF_ISO);
        if (iso != null) {
            try {
                exif.setIso(Integer.parseInt(iso.toString()));
                hasData = true;
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        Object shutterSpeed = attrs.get(MetadataKeys.EXIF_SHUTTER_SPEED);
        if (shutterSpeed != null) {
            exif.setShutterSpeed(shutterSpeed.toString());
            hasData = true;
        }
        
        return hasData ? exif : null;
    }

    private static AiAnalysisData mapAiAnalysisData(Map<String, Object> attrs) {
        Object description = attrs.get(MetadataKeys.AI_DESCRIPTION);
        Object tags = attrs.get(MetadataKeys.AI_TAGS);
        Object confidence = attrs.get(MetadataKeys.AI_CONFIDENCE);
        
        if (description == null && tags == null && confidence == null) {
            return null;
        }
        
        AiAnalysisData aiData = new AiAnalysisData();
        
        if (description != null) {
            aiData.setDescription(description.toString());
        }
        
        if (tags != null) {
            String tagsStr = tags.toString();
            List<String> tagList = Arrays.asList(tagsStr.split(","));
            aiData.setTags(tagList);
        }
        
        if (confidence != null) {
            try {
                aiData.setConfidence(Double.parseDouble(confidence.toString()));
            } catch (NumberFormatException e) {
                aiData.setConfidence(0.0);
            }
        }
        
        return aiData;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
