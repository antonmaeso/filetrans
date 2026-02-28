/**
 * Transfer API methods
 *
 * Provides methods for creating, monitoring, and managing file transfer jobs
 */

import apiClient from './client';
import type {
    CreateTransferRequest,
    TransferResponse,
    TransferListItem,
    PaginatedResponse,
} from './types';

/**
 * Create a new file transfer job
 *
 * @param request - Transfer request with source, target, and optional filters
 * @returns Transfer response with execution ID and initial status
 * @throws Error if validation fails or server error occurs
 */
export async function createTransfer(request: CreateTransferRequest): Promise<TransferResponse> {
    const response = await apiClient.post<TransferResponse>('/transfers', request);
    return response.data;
}

/**
 * Get the status of a specific transfer job
 *
 * @param executionId - The job execution ID
 * @returns Current transfer status information
 * @throws Error if transfer not found or server error occurs
 */
export async function getTransferStatus(executionId: number): Promise<TransferResponse> {
    const response = await apiClient.get<TransferResponse>(`/transfers/${executionId}`);
    return response.data;
}

/**
 * List all transfer jobs with pagination
 *
 * @param page - Page number (0-indexed), defaults to 0
 * @param size - Number of items per page, defaults to 20
 * @param sort - Sort specification, defaults to "startTime,desc"
 * @returns Paginated list of transfer jobs
 * @throws Error if server error occurs
 */
export async function listTransfers(
    page: number = 0,
    size: number = 20,
    sort: string = 'startTime,desc'
): Promise<PaginatedResponse<TransferListItem>> {
    const response = await apiClient.get<PaginatedResponse<TransferListItem>>('/transfers', {
        params: { page, size, sort },
    });
    return response.data;
}

/**
 * Delete a transfer job record
 *
 * Note: This only deletes the job record, not the transferred files
 *
 * @param executionId - The job execution ID to delete
 * @throws Error if transfer is running (409) or server error occurs
 */
export async function deleteTransfer(executionId: number): Promise<void> {
    await apiClient.delete(`/transfers/${executionId}`);
}

/**
 * Retry a failed transfer job
 *
 * Creates a new transfer job with the same parameters as the failed job
 *
 * @param executionId - The failed job execution ID
 * @returns New transfer response with new execution ID
 * @throws Error if original job is not in FAILED status (400) or server error occurs
 */
export async function retryTransfer(executionId: number): Promise<TransferResponse> {
    const response = await apiClient.post<TransferResponse>(`/transfers/${executionId}/retry`);
    return response.data;
}
