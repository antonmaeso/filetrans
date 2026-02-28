/**
 * Transfer Store - Pinia store for managing transfer job state
 *
 * Handles transfer list, status polling, and transfer operations
 * Implements 60-second cache TTL for transfer data
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { TransferListItem, TransferResponse, CreateTransferRequest } from '../api/types';
import {
    createTransfer as apiCreateTransfer,
    getTransferStatus as apiGetTransferStatus,
    listTransfers as apiListTransfers,
    deleteTransfer as apiDeleteTransfer,
    retryTransfer as apiRetryTransfer,
} from '../api/transfers';

/**
 * Cache entry for transfer data
 */
interface CachedTransfer {
    data: TransferResponse;
    timestamp: number;
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
 * Transfer store
 */
export const useTransferStore = defineStore('transfers', () => {
    // ============================================================================
    // State
    // ============================================================================

    /** List of transfers */
    const transfers = ref<TransferListItem[]>([]);

    /** Current transfer being viewed */
    const currentTransfer = ref<TransferResponse | null>(null);

    /** Pagination state */
    const pagination = ref<PaginationState>({
        page: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0,
    });

    /** Loading state */
    const loading = ref(false);

    /** Error message */
    const error = ref<string | null>(null);

    /** Polling intervals map (executionId -> intervalId) */
    const pollingIntervals = ref<Map<number, number>>(new Map());

    /** Transfer cache (executionId -> cached data) */
    const transferCache = ref<Map<number, CachedTransfer>>(new Map());

    /** Last fetch timestamp for transfer list */
    const lastFetchTimestamp = ref<number>(0);

    // ============================================================================
    // Getters
    // ============================================================================

    /** Get running transfers */
    const runningTransfers = computed(() => {
        return transfers.value.filter(
            (t: TransferListItem) => t.status === 'STARTING' || t.status === 'STARTED'
        );
    });

    /** Get completed transfers */
    const completedTransfers = computed(() => {
        return transfers.value.filter((t: TransferListItem) => t.status === 'COMPLETED');
    });

    /** Get failed transfers */
    const failedTransfers = computed(() => {
        return transfers.value.filter((t: TransferListItem) => t.status === 'FAILED');
    });

    /** Check if transfer list cache is valid */
    const isListCacheValid = computed(() => {
        return Date.now() - lastFetchTimestamp.value < CACHE_TTL;
    });

    // ============================================================================
    // Actions
    // ============================================================================

    /**
     * Load transfers with pagination
     * Uses cache if data is fresh (< 60 seconds old)
     */
    async function loadTransfers(
        page: number = 0,
        size: number = 20,
        sort: string = 'startTime,desc',
        forceRefresh: boolean = false
    ): Promise<void> {
        // Use cache if valid and not forcing refresh
        if (!forceRefresh && isListCacheValid.value && page === pagination.value.page) {
            console.log('[TransferStore] Using cached transfer list');
            return;
        }

        loading.value = true;
        error.value = null;

        try {
            const response = await apiListTransfers(page, size, sort);

            transfers.value = response.content;
            pagination.value = {
                page: response.page,
                size: response.size,
                totalPages: response.totalPages,
                totalElements: response.totalElements,
            };
            lastFetchTimestamp.value = Date.now();

            console.log(`[TransferStore] Loaded ${response.content.length} transfers`);
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to load transfers';
            error.value = message;
            console.error('[TransferStore] Load transfers error:', err);
            throw err;
        } finally {
            loading.value = false;
        }
    }

    /**
     * Load status for a specific transfer
     * Uses cache if data is fresh (< 60 seconds old)
     */
    async function loadTransferStatus(
        executionId: number,
        forceRefresh: boolean = false
    ): Promise<TransferResponse> {
        // Check cache first
        const cached = transferCache.value.get(executionId);
        if (!forceRefresh && cached && Date.now() - cached.timestamp < CACHE_TTL) {
            console.log(`[TransferStore] Using cached status for transfer ${executionId}`);
            return cached.data;
        }

        try {
            const response = await apiGetTransferStatus(executionId);

            // Update cache
            transferCache.value.set(executionId, {
                data: response,
                timestamp: Date.now(),
            });

            // Update current transfer if it matches
            if (currentTransfer.value?.executionId === executionId) {
                currentTransfer.value = response;
            }

            // Update transfer in list if present
            const index = transfers.value.findIndex(
                (t: TransferListItem) => t.executionId === executionId
            );
            if (index !== -1) {
                const existing = transfers.value[index];
                if (existing) {
                    transfers.value[index] = {
                        executionId: existing.executionId,
                        sourcePath: existing.sourcePath,
                        targetPath: existing.targetPath,
                        fileCount: existing.fileCount,
                        status: response.status,
                        startTime: response.startTime || existing.startTime,
                        endTime: response.endTime,
                    };
                }
            }

            console.log(
                `[TransferStore] Loaded status for transfer ${executionId}: ${response.status}`
            );
            return response;
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to load transfer status';
            error.value = message;
            console.error('[TransferStore] Load transfer status error:', err);
            throw err;
        }
    }

    /**
     * Create a new transfer job
     */
    async function createTransfer(request: CreateTransferRequest): Promise<TransferResponse> {
        loading.value = true;
        error.value = null;

        try {
            const response = await apiCreateTransfer(request);

            // Invalidate list cache to force refresh
            lastFetchTimestamp.value = 0;

            console.log(`[TransferStore] Created transfer ${response.executionId}`);
            return response;
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to create transfer';
            error.value = message;
            console.error('[TransferStore] Create transfer error:', err);
            throw err;
        } finally {
            loading.value = false;
        }
    }

    /**
     * Delete a transfer job record
     */
    async function deleteTransfer(executionId: number): Promise<void> {
        loading.value = true;
        error.value = null;

        try {
            await apiDeleteTransfer(executionId);

            // Remove from list
            transfers.value = transfers.value.filter(
                (t: TransferListItem) => t.executionId !== executionId
            );

            // Remove from cache
            transferCache.value.delete(executionId);

            // Clear current transfer if it matches
            if (currentTransfer.value?.executionId === executionId) {
                currentTransfer.value = null;
            }

            // Update pagination
            pagination.value.totalElements = Math.max(0, pagination.value.totalElements - 1);

            console.log(`[TransferStore] Deleted transfer ${executionId}`);
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to delete transfer';
            error.value = message;
            console.error('[TransferStore] Delete transfer error:', err);
            throw err;
        } finally {
            loading.value = false;
        }
    }

    /**
     * Retry a failed transfer job
     */
    async function retryTransfer(executionId: number): Promise<TransferResponse> {
        loading.value = true;
        error.value = null;

        try {
            const response = await apiRetryTransfer(executionId);

            // Invalidate list cache to force refresh
            lastFetchTimestamp.value = 0;

            console.log(
                `[TransferStore] Retried transfer ${executionId}, new ID: ${response.executionId}`
            );
            return response;
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to retry transfer';
            error.value = message;
            console.error('[TransferStore] Retry transfer error:', err);
            throw err;
        } finally {
            loading.value = false;
        }
    }

    /**
     * Start polling for a transfer's status
     * Polls every 3 seconds for running transfers
     */
    function startPolling(executionId: number, intervalMs: number = 3000): void {
        // Don't start if already polling
        if (pollingIntervals.value.has(executionId)) {
            console.log(`[TransferStore] Already polling transfer ${executionId}`);
            return;
        }

        console.log(`[TransferStore] Starting polling for transfer ${executionId}`);

        const intervalId = window.setInterval(async () => {
            try {
                const status = await loadTransferStatus(executionId, true);

                // Stop polling if transfer is no longer running
                if (status.status !== 'STARTING' && status.status !== 'STARTED') {
                    console.log(
                        `[TransferStore] Transfer ${executionId} finished, stopping polling`
                    );
                    stopPolling(executionId);
                }
            } catch (err) {
                console.error(`[TransferStore] Polling error for transfer ${executionId}:`, err);
                // Continue polling even on error
            }
        }, intervalMs);

        pollingIntervals.value.set(executionId, intervalId);
    }

    /**
     * Stop polling for a transfer's status
     */
    function stopPolling(executionId: number): void {
        const intervalId = pollingIntervals.value.get(executionId);
        if (intervalId !== undefined) {
            window.clearInterval(intervalId);
            pollingIntervals.value.delete(executionId);
            console.log(`[TransferStore] Stopped polling for transfer ${executionId}`);
        }
    }

    /**
     * Stop all polling intervals
     */
    function stopAllPolling(): void {
        pollingIntervals.value.forEach(intervalId => {
            window.clearInterval(intervalId);
        });
        pollingIntervals.value.clear();
        console.log('[TransferStore] Stopped all polling');
    }

    /**
     * Clear error state
     */
    function clearError(): void {
        error.value = null;
    }

    /**
     * Set current transfer
     */
    function setCurrentTransfer(transfer: TransferResponse | null): void {
        currentTransfer.value = transfer;
    }

    /**
     * Invalidate cache for a specific transfer
     */
    function invalidateCache(executionId: number): void {
        transferCache.value.delete(executionId);
    }

    /**
     * Invalidate all caches
     */
    function invalidateAllCaches(): void {
        transferCache.value.clear();
        lastFetchTimestamp.value = 0;
    }

    // ============================================================================
    // Return store interface
    // ============================================================================

    return {
        // State
        transfers,
        currentTransfer,
        pagination,
        loading,
        error,
        pollingIntervals,

        // Getters
        runningTransfers,
        completedTransfers,
        failedTransfers,
        isListCacheValid,

        // Actions
        loadTransfers,
        loadTransferStatus,
        createTransfer,
        deleteTransfer,
        retryTransfer,
        startPolling,
        stopPolling,
        stopAllPolling,
        clearError,
        setCurrentTransfer,
        invalidateCache,
        invalidateAllCaches,
    };
});
