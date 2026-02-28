package com.ant.filetrans.metadata.web;

import com.ant.filetrans.metadata.api.model.FileListResponse;
import com.ant.filetrans.metadata.api.model.FileMetadataResponse;
import com.ant.filetrans.metadata.api.model.PagedFileListResponse;
import com.ant.filetrans.metadata.application.FileService;
import com.ant.filetrans.metadata.application.SearchService;
import com.ant.filetrans.metadata.application.ThumbnailService;
import com.ant.filetrans.metadata.web.mapper.MetadataApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for file operations including listing, metadata retrieval,
 * thumbnails, and search.
 *
 * Resource: /files
 * - GET /files                    → list files with pagination and filtering
 * - GET /files/{id}/metadata      → get file metadata from sidecar
 * - GET /files/{id}/thumbnail     → get file thumbnail image
 * - GET /files/search             → search files by query, tags, and dates
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FilesController {

    private final FileService fileService;
    private final SearchService searchService;
    private final ThumbnailService thumbnailService;
    
    @Value("${filetrans.target.base-dir:/tmp/filetrans-target}")
    private String targetBaseDir;

    /**
     * List transferred files with pagination and filtering.
     *
     * GET /files?page=0&size=50&dateFrom=2024-01-01&dateTo=2024-12-31&directory=2024/2024-01-15
     */
    @GetMapping
    public ResponseEntity<PagedFileListResponse> listFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) String directory) {
        
        log.debug("Listing files: page={}, size={}, dateFrom={}, dateTo={}, directory={}", 
                  page, size, dateFrom, dateTo, directory);
        
        Path baseDir = Paths.get(targetBaseDir);
        List<FileService.FileInfo> allFiles = fileService.listFiles(baseDir, dateFrom, dateTo, directory);
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, allFiles.size());
        List<FileService.FileInfo> pageFiles = start < allFiles.size() 
            ? allFiles.subList(start, end) 
            : List.of();
        
        // Map to DTOs
        List<FileListResponse> content = pageFiles.stream()
            .map(MetadataApiMapper::toFileListResponse)
            .toList();
        
        // Build paginated response
        PagedFileListResponse response = new PagedFileListResponse();
        response.setContent(content);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements((long) allFiles.size());
        response.setTotalPages((int) Math.ceil((double) allFiles.size() / size));
        
        log.debug("Returning {} files out of {} total", content.size(), allFiles.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get file metadata from sidecar.
     *
     * GET /files/{id}/metadata
     */
    @GetMapping("/{id}/metadata")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(@PathVariable String id) {
        log.debug("Fetching metadata for file ID: {}", id);
        
        Path baseDir = Paths.get(targetBaseDir);
        Path file = fileService.getFileById(baseDir, id);
        
        if (file == null) {
            log.warn("File not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        try {
            FileMetadataResponse response = MetadataApiMapper.toFileMetadataResponse(file, id);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to read metadata for file {}", file, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get file thumbnail.
     *
     * GET /files/{id}/thumbnail
     */
    @GetMapping(value = "/{id}/thumbnail", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getFileThumbnail(@PathVariable String id) {
        log.debug("Fetching thumbnail for file ID: {}", id);
        
        Path baseDir = Paths.get(targetBaseDir);
        Path file = fileService.getFileById(baseDir, id);
        
        if (file == null) {
            log.warn("File not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        // Check if file is an image
        String filename = file.getFileName().toString().toLowerCase();
        if (!filename.endsWith(".jpg") && !filename.endsWith(".jpeg") && !filename.endsWith(".png")) {
            log.warn("File is not an image: {}", file);
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] thumbnail = thumbnailService.getThumbnail(file);
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(thumbnail);
        } catch (IOException e) {
            log.error("Failed to generate thumbnail for file {}", file, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search files by various criteria.
     *
     * GET /files/search?q=vacation&tags=beach,sunset&dateFrom=2024-01-01&dateTo=2024-12-31&page=0&size=50
     */
    @GetMapping("/search")
    public ResponseEntity<PagedFileListResponse> searchFiles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        log.debug("Searching files: q={}, tags={}, dateFrom={}, dateTo={}, page={}, size={}", 
                  q, tags, dateFrom, dateTo, page, size);
        
        Path baseDir = Paths.get(targetBaseDir);
        List<FileService.FileInfo> allResults = searchService.search(baseDir, q, tags, dateFrom, dateTo);
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, allResults.size());
        List<FileService.FileInfo> pageResults = start < allResults.size() 
            ? allResults.subList(start, end) 
            : List.of();
        
        // Map to DTOs
        List<FileListResponse> content = pageResults.stream()
            .map(MetadataApiMapper::toFileListResponse)
            .toList();
        
        // Build paginated response
        PagedFileListResponse response = new PagedFileListResponse();
        response.setContent(content);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements((long) allResults.size());
        response.setTotalPages((int) Math.ceil((double) allResults.size() / size));
        
        log.debug("Returning {} search results out of {} total", content.size(), allResults.size());
        return ResponseEntity.ok(response);
    }
}
