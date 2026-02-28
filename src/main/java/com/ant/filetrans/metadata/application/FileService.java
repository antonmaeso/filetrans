package com.ant.filetrans.metadata.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for file operations including listing, filtering, and organizing files.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    /**
     * List files in a directory with optional filtering.
     *
     * @param baseDir the base directory to scan
     * @param dateFrom optional start date filter
     * @param dateTo optional end date filter
     * @param directory optional specific directory filter
     * @return list of file information
     */
    public List<FileInfo> listFiles(Path baseDir, LocalDate dateFrom, LocalDate dateTo, String directory) {
        List<FileInfo> files = new ArrayList<>();
        
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            log.warn("Base directory does not exist or is not a directory: {}", baseDir);
            return files;
        }

        Path searchPath = directory != null ? baseDir.resolve(directory) : baseDir;
        
        if (!Files.exists(searchPath)) {
            log.warn("Search path does not exist: {}", searchPath);
            return files;
        }

        try (Stream<Path> paths = Files.walk(searchPath)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> !p.getFileName().toString().endsWith(".metadata.json"))
                .forEach(path -> {
                    try {
                        Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                        LocalDate fileDate = lastModified.atZone(ZoneId.systemDefault()).toLocalDate();
                        
                        // Apply date filters
                        if (dateFrom != null && fileDate.isBefore(dateFrom)) {
                            return;
                        }
                        if (dateTo != null && fileDate.isAfter(dateTo)) {
                            return;
                        }
                        
                        long size = Files.size(path);
                        String id = generateFileId(path);
                        
                        files.add(new FileInfo(
                            id,
                            path.toString(),
                            path.getFileName().toString(),
                            size,
                            lastModified,
                            isImage(path)
                        ));
                    } catch (IOException e) {
                        log.warn("Failed to read file info for {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to walk directory {}", searchPath, e);
        }

        return files;
    }

    /**
     * Get file by ID.
     *
     * @param baseDir the base directory
     * @param fileId the file identifier
     * @return file path or null if not found
     */
    public Path getFileById(Path baseDir, String fileId) {
        // For now, we'll use a simple approach: scan and match
        // In production, you might want to use a database index
        try (Stream<Path> paths = Files.walk(baseDir)) {
            return paths.filter(Files::isRegularFile)
                .filter(p -> !p.getFileName().toString().endsWith(".metadata.json"))
                .filter(p -> generateFileId(p).equals(fileId))
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            log.error("Failed to find file by ID {}", fileId, e);
            return null;
        }
    }

    /**
     * Search files by query text, tags, and date range.
     *
     * @param baseDir the base directory
     * @param query text query to search in filenames
     * @param tags comma-separated tags to filter by
     * @param dateFrom optional start date
     * @param dateTo optional end date
     * @return list of matching files
     */
    public List<FileInfo> searchFiles(Path baseDir, String query, String tags, LocalDate dateFrom, LocalDate dateTo) {
        List<FileInfo> allFiles = listFiles(baseDir, dateFrom, dateTo, null);
        
        if (query == null && tags == null) {
            return allFiles;
        }

        return allFiles.stream()
            .filter(file -> matchesQuery(file, query))
            .toList();
    }

    private boolean matchesQuery(FileInfo file, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        return file.filename().toLowerCase().contains(lowerQuery);
    }

    private boolean isImage(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".jpg") || 
               filename.endsWith(".jpeg") || 
               filename.endsWith(".png");
    }

    private String generateFileId(Path path) {
        // Generate a consistent ID based on the file path
        // Using UUID v5 (name-based) would be better, but for simplicity we'll use a hash
        return UUID.nameUUIDFromBytes(path.toString().getBytes()).toString();
    }

    /**
     * File information record.
     */
    public record FileInfo(
        String id,
        String path,
        String filename,
        long size,
        Instant transferDate,
        boolean isImage
    ) {}
}
