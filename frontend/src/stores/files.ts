/**
 * File Store - Pinia store for managing file browsing and metadata state
 *
 * Handles file list, date hierarchy, search, and metadata caching
 * Implements 60-second cache TTL for metadata
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { FileListItem, FileMetadata, SearchParams } from '../api/types';
import {
    listFiles as apiListFiles,
    getFileMetadata as apiGetFileMetadata,
    searchFiles as apiSearchFiles,
} from '../api/files';

/**
 * Cache entry for file metadata
 */
interface CachedMetadata {
    data: FileMetadata;
    timestamp: number;
}

/**
 * Date hierarchy structure: YYYY -> YYYY-MM-DD -> files
 */
interface DateHierarchy {
    [year: string]: {
        [date: string]: FileListItem[];
    };
}

/**
 * Pagination state
 */
interface PaginationState {
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
}

/**
 * Cache TTL in milliseconds (60 seconds)
 */
const CACHE_TTL = 60000;

/**
 * File store
 */
export const useFileStore = defineStore('files', () => {
    // ============================================================================
    // State
    // ============================================================================

    /** List of files */
    const files = ref<FileListItem[]>([]);

    /** File tree organized by date hierarchy (YYYY/YYYY-MM-DD) */
    const fileTree = ref<DateHierarchy>({});

    /** Current file being viewed */
    const currentFile = ref<FileMetadata | null>(null);

    /** Search results */
    const searchResults = ref<FileListItem[]>([]);

    /** Pagination state */
    const pagination = ref<PaginationState>({
        page: 0,
        size: 50,
        totalPages: 0,
        totalElements: 0,
    });

    /** Loading state */
    const loading = ref(false);

    /** Error message */
    const error = ref<string | null>(null);

    /** Metadata cache (fileId -> cached data) */
    const metadataCache = ref<Map<string, CachedMetadata>>(new Map());

    /** Last fetch timestamp for file list */
    const lastFetchTimestamp = ref<number>(0);

    // ============================================================================
    // Getters
    // ============================================================================

    /** Check if file list cache is valid */
    const isListCacheValid = computed(() => {
        return Date.now() - lastFetchTimestamp.value < CACHE_TTL;
    });

    /** Get years from file tree (sorted descending) */
    const years = computed(() => {
        return Object.keys(fileTree.value).sort((a, b) => b.localeCompare(a));
    });

    /** Get total file count */
    const totalFiles = computed(() => {
        return pagination.value.totalElements;
    });

    /** Check if search is active */
    const hasSearchResults = computed(() => {
        return searchResults.value.length > 0;
    });

    // ============================================================================
    // Actions
    // ============================================================================

    /**
     * Load files with pagination and optional filtering
     * Uses cache if data is fresh (< 60 seconds old)
     */
    async function loadFiles(
        page: number = 0,
        size: number = 50,
        dateFrom?: string,
        dateTo?: string,
        directory?: string,
        forceRefresh: boolean = false
    ): Promise<void> {
        // Use cache if valid and not forcing refresh
        if (!forceRefresh && isListCacheValid.value && page === pagination.value.page) {
            console.log('[FileStore] Using cached file list');
            return;
        }

        loading.value = true;
        error.value = null;

        try {
            const response = await apiListFiles(page, size, dateFrom, dateTo, directory);

            files.value = response.content;
            pagination.value = {
                page: response.page,
                size: response.size,
                totalPages: response.totalPages,
                totalElements: response.totalElements,
            };
            lastFetchTimestamp.value = Date.now();

            // Build date hierarchy from loaded files
            buildDateHierarchy(response.content);

            console.log(`[FileStore] Loaded ${response.content.length} files`);
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to load files';
            error.value = message;
            console.error('[FileStore] Load files error:', err);
            throw err;
        } finally {
            loading.value = false;
        }
    }

    /**
     * Load metadata for a specific file
     * Uses cache if data is fresh (< 60 seconds old)
     */
    async function loadFileMetadata(
        id: string,
        forceRefresh: boolean = false
    ): Promise<FileMetadata> {
        // Check cache first
        const cached = metadataCache.value.get(id);
        if (!forceRefresh && cached && Date.now() - cached.timestamp < CACHE_TTL) {
            console.log(`[FileStore] Using cached metadata for file ${id}`);
            return cached.data;
        }

        try {
            const response = await apiGetFileMetadata(id);

            // Update cache
            metadataCache.value.set(id, {
                data: response,
                timestamp: Date.now(),
            });

            // Update current file if it matches
            if (currentFile.value?.id === id) {
                currentFile.value = response;
            }

            console.log(`[FileStore] Loaded metadata for file ${id}`);
            return response;
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to load file metadata';
            error.value = message;
            console.error('[FileStore] Load file metadata error:', err);
            throw err;
        }
    }

    /**
     * Search files by text query, tags, and date range
     */
    async function searchFiles(params: SearchParams): Promise<void> {
        loading.value = true;
        error.value = null;

        try {
            const response = await apiSearchFiles(params);

            searchResults.value = response.content;
            pagination.value = {
                page: response.page,
                size: response.size,
                totalPages: response.totalPages,
                totalElements: response.totalElements,
            };

            console.log(`[FileStore] Search returned ${response.content.length} results`);
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to search files';
            error.value = message;
            console.error('[FileStore] Search files error:', err);
            throw err;
        } finally {
            loading.value = false;
        }
    }

    /**
     * Build date hierarchy from file list
     * Organizes files into YYYY/YYYY-MM-DD structure
     */
    function buildDateHierarchy(fileList: FileListItem[]): void {
        const hierarchy: DateHierarchy = {};

        for (const file of fileList) {
            try {
                // Parse transfer date (ISO 8601 format)
                const date = new Date(file.transferDate);
                const year = date.getFullYear().toString();
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const day = String(date.getDate()).padStart(2, '0');
                const dateKey = `${year}-${month}-${day}`;

                // Initialize year if not exists
                if (!hierarchy[year]) {
                    hierarchy[year] = {};
                }

                // Initialize date if not exists
                if (!hierarchy[year][dateKey]) {
                    hierarchy[year][dateKey] = [];
                }

                // Add file to date bucket
                hierarchy[year][dateKey].push(file);
            } catch (err) {
                console.warn(`[FileStore] Failed to parse date for file ${file.id}:`, err);
            }
        }

        fileTree.value = hierarchy;
        console.log(`[FileStore] Built date hierarchy with ${Object.keys(hierarchy).length} years`);
    }

    /**
     * Get files for a specific date
     */
    function getFilesForDate(year: string, date: string): FileListItem[] {
        return fileTree.value[year]?.[date] || [];
    }

    /**
     * Get dates for a specific year (sorted descending)
     */
    function getDatesForYear(year: string): string[] {
        const dates = Object.keys(fileTree.value[year] || {});
        return dates.sort((a, b) => b.localeCompare(a));
    }

    /**
     * Clear search results
     */
    function clearSearch(): void {
        searchResults.value = [];
    }

    /**
     * Clear error state
     */
    function clearError(): void {
        error.value = null;
    }

    /**
     * Set current file
     */
    function setCurrentFile(file: FileMetadata | null): void {
        currentFile.value = file;
    }

    /**
     * Invalidate cache for a specific file
     */
    function invalidateCache(id: string): void {
        metadataCache.value.delete(id);
    }

    /**
     * Invalidate all caches
     */
    function invalidateAllCaches(): void {
        metadataCache.value.clear();
        lastFetchTimestamp.value = 0;
    }

    // ============================================================================
    // Return store interface
    // ============================================================================

    return {
        // State
        files,
        fileTree,
        currentFile,
        searchResults,
        pagination,
        loading,
        error,
        metadataCache,

        // Getters
        isListCacheValid,
        years,
        totalFiles,
        hasSearchResults,

        // Actions
        loadFiles,
        loadFileMetadata,
        searchFiles,
        buildDateHierarchy,
        getFilesForDate,
        getDatesForYear,
        clearSearch,
        clearError,
        setCurrentFile,
        invalidateCache,
        invalidateAllCaches,
    };
});
