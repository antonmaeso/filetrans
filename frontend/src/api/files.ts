/**
 * File API methods
 *
 * Provides methods for browsing, searching, and retrieving file metadata
 */

import apiClient from './client';
import type { FileListItem, FileMetadata, PaginatedResponse, SearchParams } from './types';

/**
 * List transferred files with optional filtering
 *
 * @param page - Page number (0-indexed), defaults to 0
 * @param size - Number of items per page, defaults to 50
 * @param dateFrom - Optional start date for filtering (ISO 8601)
 * @param dateTo - Optional end date for filtering (ISO 8601)
 * @param directory - Optional directory path to filter by
 * @returns Paginated list of files organized by date structure
 * @throws Error if server error occurs
 */
export async function listFiles(
    page: number = 0,
    size: number = 50,
    dateFrom?: string,
    dateTo?: string,
    directory?: string
): Promise<PaginatedResponse<FileListItem>> {
    const response = await apiClient.get<PaginatedResponse<FileListItem>>('/files', {
        params: {
            page,
            size,
            ...(dateFrom && { dateFrom }),
            ...(dateTo && { dateTo }),
            ...(directory && { directory }),
        },
    });
    return response.data;
}

/**
 * Get detailed metadata for a specific file
 *
 * Retrieves metadata from the sidecar JSON file including EXIF data and AI analysis
 *
 * @param id - The unique file identifier
 * @returns Complete file metadata including EXIF and AI analysis
 * @throws Error if file not found (404) or sidecar doesn't exist
 */
export async function getFileMetadata(id: string): Promise<FileMetadata> {
    const response = await apiClient.get<FileMetadata>(`/files/${id}/metadata`);
    return response.data;
}

/**
 * Get the thumbnail URL for an image file
 *
 * Returns a URL that can be used in img src attributes to display thumbnails
 *
 * @param id - The unique file identifier
 * @returns URL string for the thumbnail endpoint
 */
export function getThumbnailUrl(id: string): string {
    const baseURL = apiClient.defaults.baseURL || '';
    return `${baseURL}/files/${id}/thumbnail`;
}

/**
 * Search files by text query, tags, and date range
 *
 * Searches in filenames, AI descriptions, and AI tags
 *
 * @param params - Search parameters including query text, tags, and date filters
 * @returns Paginated list of files matching search criteria
 * @throws Error if server error occurs
 */
export async function searchFiles(params: SearchParams): Promise<PaginatedResponse<FileListItem>> {
    const response = await apiClient.get<PaginatedResponse<FileListItem>>('/files/search', {
        params: {
            ...(params.q && { q: params.q }),
            ...(params.tags && params.tags.length > 0 && { tags: params.tags.join(',') }),
            ...(params.dateFrom && { dateFrom: params.dateFrom }),
            ...(params.dateTo && { dateTo: params.dateTo }),
            page: params.page ?? 0,
            size: params.size ?? 50,
        },
    });
    return response.data;
}
