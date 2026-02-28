/**
 * TypeScript type definitions for the File Transfer API
 *
 * These interfaces match the backend OpenAPI specifications and design document.
 */

// ============================================================================
// Transfer Types
// ============================================================================

/**
 * Request to create a new file transfer job
 * Either sourceDir (for bulk transfer) or filePath (for single file) must be provided
 */
export interface CreateTransferRequest {
    /** Source directory for bulk transfer (mutually exclusive with filePath) */
    sourceDir?: string;
    /** Destination base directory where files will be organized by date */
    targetBaseDir: string;
    /** Single file path for individual transfer (mutually exclusive with sourceDir) */
    filePath?: string;
    /** Optional list of file extensions to filter (case-insensitive) */
    extensions?: string[];
}

/**
 * Transfer job status information
 */
export interface TransferResponse {
    /** Unique job execution ID */
    executionId: number;
    /** Current job status */
    status: TransferStatus;
    /** Job start timestamp (ISO 8601) */
    startTime?: string;
    /** Job end timestamp (ISO 8601) */
    endTime?: string;
}

/**
 * Transfer job status enum
 */
export type TransferStatus =
    | 'STARTING'
    | 'STARTED'
    | 'STOPPING'
    | 'STOPPED'
    | 'FAILED'
    | 'COMPLETED'
    | 'ABANDONED'
    | 'UNKNOWN';

/**
 * Transfer list item with additional details for history view
 */
export interface TransferListItem {
    /** Unique job execution ID */
    executionId: number;
    /** Source path (directory or file) */
    sourcePath: string;
    /** Target base directory path */
    targetPath: string;
    /** Current job status */
    status: TransferStatus;
    /** Job start timestamp (ISO 8601) */
    startTime: string;
    /** Job end timestamp (ISO 8601) */
    endTime?: string;
    /** Number of files transferred */
    fileCount: number;
}

// ============================================================================
// Pagination Types
// ============================================================================

/**
 * Generic paginated response wrapper
 */
export interface PaginatedResponse<T> {
    /** Array of items for the current page */
    content: T[];
    /** Current page number (0-indexed) */
    page: number;
    /** Number of items per page */
    size: number;
    /** Total number of pages */
    totalPages: number;
    /** Total number of items across all pages */
    totalElements: number;
}

// ============================================================================
// File Types
// ============================================================================

/**
 * File list item for browser and search results
 */
export interface FileListItem {
    /** Unique file identifier */
    id: string;
    /** Full file path */
    path: string;
    /** File name only */
    filename: string;
    /** File size in bytes */
    size: number;
    /** Transfer date timestamp (ISO 8601) */
    transferDate: string;
    /** Optional thumbnail URL for images */
    thumbnailUrl?: string;
}

/**
 * Complete file metadata including EXIF and AI analysis
 */
export interface FileMetadata {
    /** Unique file identifier */
    id: string;
    /** Full file path */
    path: string;
    /** File name only */
    filename: string;
    /** File size in bytes */
    size: number;
    /** Transfer date timestamp (ISO 8601) */
    transferDate: string;
    /** Original source path before transfer */
    sourcePath: string;
    /** File hash value */
    hash: string;
    /** Hash algorithm used (e.g., MD5, SHA-256) */
    hashAlgorithm: string;
    /** EXIF data if available (for images) */
    exif?: ExifData;
    /** AI analysis results if available */
    aiAnalysis?: AiAnalysisData;
}

/**
 * EXIF metadata extracted from images
 */
export interface ExifData {
    /** Camera model name */
    cameraModel?: string;
    /** Image capture date timestamp (ISO 8601) */
    captureDate?: string;
    /** GPS latitude coordinate */
    gpsLatitude?: number;
    /** GPS longitude coordinate */
    gpsLongitude?: number;
    /** Focal length (e.g., "50mm") */
    focalLength?: string;
    /** Aperture value (e.g., "f/2.8") */
    aperture?: string;
    /** ISO sensitivity value */
    iso?: number;
    /** Shutter speed (e.g., "1/250") */
    shutterSpeed?: string;
}

/**
 * AI-generated analysis data
 */
export interface AiAnalysisData {
    /** AI-generated description of the image */
    description: string;
    /** Array of AI-generated tags */
    tags: string[];
    /** Confidence score (0.0 to 1.0) */
    confidence: number;
}

// ============================================================================
// Search Types
// ============================================================================

/**
 * Search parameters for file search endpoint
 */
export interface SearchParams {
    /** Text query to search in filenames, descriptions, and tags */
    q?: string;
    /** Array of tags to filter by */
    tags?: string[];
    /** Start date for date range filter (ISO 8601) */
    dateFrom?: string;
    /** End date for date range filter (ISO 8601) */
    dateTo?: string;
    /** Page number (0-indexed) */
    page?: number;
    /** Number of items per page */
    size?: number;
}

// ============================================================================
// Error Types
// ============================================================================

/**
 * Standard API error response
 */
export interface ApiError {
    /** Error timestamp (ISO 8601) */
    timestamp: string;
    /** HTTP status code */
    status: number;
    /** Error type/name */
    error: string;
    /** Detailed error message */
    message: string;
    /** Request path that caused the error */
    path: string;
}
