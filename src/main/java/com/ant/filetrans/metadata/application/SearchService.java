package com.ant.filetrans.metadata.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.ant.filetrans.metadata.api.FileMetadata;
import com.ant.filetrans.metadata.api.MetadataKeys;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for searching files based on metadata, AI analysis, and other criteria.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ObjectMapper objectMapper;

    /**
     * Search files by query text, tags, and date range.
     * Searches in filenames, AI descriptions, and AI tags from sidecar files.
     *
     * @param baseDir the base directory to search
     * @param query text query to search
     * @param tags comma-separated tags to filter by
     * @param dateFrom optional start date
     * @param dateTo optional end date
     * @return list of matching file information
     */
    public List<FileService.FileInfo> search(Path baseDir, String query, String tags, LocalDate dateFrom, LocalDate dateTo) {
        List<FileService.FileInfo> results = new ArrayList<>();
        
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            log.warn("Base directory does not exist: {}", baseDir);
            return results;
        }

        List<String> searchTags = tags != null ? Arrays.asList(tags.split(",")) : List.of();
        String lowerQuery = query != null ? query.toLowerCase() : null;

        try (Stream<Path> paths = Files.walk(baseDir)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> !p.getFileName().toString().endsWith(".metadata.json"))
                .forEach(path -> {
                    try {
                        if (matchesSearchCriteria(path, lowerQuery, searchTags, dateFrom, dateTo)) {
                            long size = Files.size(path);
                            String id = java.util.UUID.nameUUIDFromBytes(path.toString().getBytes()).toString();
                            
                            results.add(new FileService.FileInfo(
                                id,
                                path.toString(),
                                path.getFileName().toString(),
                                size,
                                Files.getLastModifiedTime(path).toInstant(),
                                isImage(path)
                            ));
                        }
                    } catch (IOException e) {
                        log.warn("Failed to process file {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to search directory {}", baseDir, e);
        }

        return results;
    }

    private boolean matchesSearchCriteria(Path file, String query, List<String> tags, LocalDate dateFrom, LocalDate dateTo) {
        try {
            // Check date range
            if (dateFrom != null || dateTo != null) {
                LocalDate fileDate = Files.getLastModifiedTime(file)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                
                if (dateFrom != null && fileDate.isBefore(dateFrom)) {
                    return false;
                }
                if (dateTo != null && fileDate.isAfter(dateTo)) {
                    return false;
                }
            }

            // Check filename
            String filename = file.getFileName().toString().toLowerCase();
            if (query != null && filename.contains(query)) {
                return true;
            }

            // Check metadata sidecar for AI description and tags
            Path sidecarPath = file.resolveSibling(file.getFileName() + ".metadata.json");
            if (Files.exists(sidecarPath)) {
                FileMetadata metadata = objectMapper.readValue(sidecarPath.toFile(), FileMetadata.class);
                
                // Check AI description
                if (query != null && metadata.attributes() != null) {
                    Object aiDescription = metadata.attributes().get(MetadataKeys.AI_DESCRIPTION);
                    if (aiDescription != null && aiDescription.toString().toLowerCase().contains(query)) {
                        return true;
                    }
                }

                // Check AI tags
                if (!tags.isEmpty() && metadata.attributes() != null) {
                    Object aiTags = metadata.attributes().get(MetadataKeys.AI_TAGS);
                    if (aiTags != null) {
                        String tagsStr = aiTags.toString().toLowerCase();
                        for (String tag : tags) {
                            if (tagsStr.contains(tag.toLowerCase().trim())) {
                                return true;
                            }
                        }
                    }
                }
            }

            // If query is null and no tags specified, match all files in date range
            return query == null && tags.isEmpty();
            
        } catch (IOException e) {
            log.warn("Failed to check search criteria for {}", file, e);
            return false;
        }
    }

    private boolean isImage(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".jpg") || 
               filename.endsWith(".jpeg") || 
               filename.endsWith(".png");
    }
}
