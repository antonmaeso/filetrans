package com.ant.filetrans.transfer.web.dto;

import java.util.List;

import com.ant.filetrans.transfer.api.model.TransferListResponse;

/**
 * Paginated response wrapper for transfer list.
 * Contains the list of transfers and pagination metadata.
 */
public record PagedTransferListResponse(
        List<TransferListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /**
     * Creates a paginated response from a list of transfers.
     *
     * @param content the list of transfers for this page
     * @param page zero-based page number
     * @param size number of items per page
     * @param totalElements total number of transfers across all pages
     * @return paginated response
     */
    public static PagedTransferListResponse of(List<TransferListResponse> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PagedTransferListResponse(content, page, size, totalElements, totalPages);
    }
}
