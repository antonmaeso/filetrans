package com.ant.filetrans.metadata.application;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating and caching image thumbnails.
 */
@Slf4j
@Service
public class ThumbnailService {

    private static final int MAX_THUMBNAIL_SIZE = 200;
    private static final String THUMBNAIL_FORMAT = "jpg";
    
    // Simple in-memory cache for thumbnails
    private final ConcurrentHashMap<String, byte[]> thumbnailCache = new ConcurrentHashMap<>();

    /**
     * Generate or retrieve a thumbnail for an image file.
     *
     * @param imagePath path to the image file
     * @return thumbnail image as byte array
     * @throws IOException if thumbnail generation fails
     */
    public byte[] getThumbnail(Path imagePath) throws IOException {
        String cacheKey = imagePath.toString();
        
        // Check cache first
        byte[] cached = thumbnailCache.get(cacheKey);
        if (cached != null) {
            log.debug("Returning cached thumbnail for {}", imagePath);
            return cached;
        }

        // Generate thumbnail
        log.debug("Generating thumbnail for {}", imagePath);
        byte[] thumbnail = generateThumbnail(imagePath);
        
        // Cache it
        thumbnailCache.put(cacheKey, thumbnail);
        
        return thumbnail;
    }

    private byte[] generateThumbnail(Path imagePath) throws IOException {
        if (!Files.exists(imagePath)) {
            throw new IOException("Image file not found: " + imagePath);
        }

        // Read the original image
        BufferedImage originalImage = ImageIO.read(imagePath.toFile());
        if (originalImage == null) {
            throw new IOException("Failed to read image: " + imagePath);
        }

        // Calculate thumbnail dimensions maintaining aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        int thumbnailWidth;
        int thumbnailHeight;
        
        if (originalWidth > originalHeight) {
            thumbnailWidth = MAX_THUMBNAIL_SIZE;
            thumbnailHeight = (int) ((double) originalHeight / originalWidth * MAX_THUMBNAIL_SIZE);
        } else {
            thumbnailHeight = MAX_THUMBNAIL_SIZE;
            thumbnailWidth = (int) ((double) originalWidth / originalHeight * MAX_THUMBNAIL_SIZE);
        }

        // Create thumbnail
        BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        
        // Use high-quality rendering
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        graphics.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
        graphics.dispose();

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, THUMBNAIL_FORMAT, baos);
        
        return baos.toByteArray();
    }

    /**
     * Clear the thumbnail cache.
     */
    public void clearCache() {
        thumbnailCache.clear();
        log.info("Thumbnail cache cleared");
    }
}
