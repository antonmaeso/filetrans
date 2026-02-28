<template>
    <div class="transfer-history">
        <h1>Transfer History</h1>

        <LoadingSpinner v-if="transfersStore.loading" message="Loading transfers..." />
        <ErrorMessage 
            v-else-if="transfersStore.error" 
            :message="transfersStore.error"
            :retryable="true"
            @retry="loadTransfers"
        />

        <div v-else>
            <div v-if="transfersStore.transfers.length === 0" class="no-transfers">
                <p>No transfers found. <router-link to="/transfers/new">Create your first transfer</router-link></p>
            </div>

            <div v-else>
                <table class="transfers-table">
                    <thead>
                        <tr>
                            <th>Execution ID</th>
                            <th>Source</th>
                            <th>Target</th>
                            <th>Status</th>
                            <th>Start Time</th>
                            <th>End Time</th>
                            <th>Files</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr 
                            v-for="transfer in transfersStore.transfers" 
                            :key="transfer.executionId"
                            class="transfer-row"
                            @click="navigateToDetails(transfer.executionId)"
                        >
                            <td data-label="Execution ID">{{ transfer.executionId }}</td>
                            <td data-label="Source" class="path-cell">{{ transfer.sourcePath }}</td>
                            <td data-label="Target" class="path-cell">{{ transfer.targetPath }}</td>
                            <td data-label="Status">
                                <TransferStatusBadge :status="transfer.status" />
                            </td>
                            <td data-label="Start Time">{{ formatDate(transfer.startTime) }}</td>
                            <td data-label="End Time">{{ transfer.endTime ? formatDate(transfer.endTime) : '-' }}</td>
                            <td data-label="Files">{{ transfer.fileCount }}</td>
                            <td class="actions-cell" @click.stop>
                                <button 
                                    v-if="transfer.status === 'FAILED'" 
                                    @click="retryTransfer(transfer.executionId)"
                                    class="btn-retry"
                                    title="Retry transfer"
                                >
                                    Retry
                                </button>
                                <button 
                                    v-if="transfer.status === 'COMPLETED' || transfer.status === 'FAILED'" 
                                    @click="deleteTransfer(transfer.executionId)"
                                    class="btn-delete"
                                    title="Delete transfer"
                                >
                                    Delete
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>

                <div class="pagination" v-if="transfersStore.pagination.totalPages > 1">
                    <button 
                        @click="goToPage(transfersStore.pagination.page - 1)"
                        :disabled="transfersStore.pagination.page === 0"
                        class="btn-page"
                    >
                        Previous
                    </button>
                    <span class="page-info">
                        Page {{ transfersStore.pagination.page + 1 }} of {{ transfersStore.pagination.totalPages }}
                    </span>
                    <button 
                        @click="goToPage(transfersStore.pagination.page + 1)"
                        :disabled="transfersStore.pagination.page >= transfersStore.pagination.totalPages - 1"
                        class="btn-page"
                    >
                        Next
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useTransferStore } from '../stores/transfers';
import { formatDate } from '../utils/formatters';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import ErrorMessage from '../components/ErrorMessage.vue';
import TransferStatusBadge from '../components/TransferStatusBadge.vue';

const router = useRouter();
const transfersStore = useTransferStore();

const loadTransfers = async () => {
    await transfersStore.loadTransfers();
};

const goToPage = async (page: number) => {
    await transfersStore.loadTransfers(page);
};

const navigateToDetails = (executionId: number) => {
    router.push(`/transfers/${executionId}`);
};

const retryTransfer = async (executionId: number) => {
    if (confirm('Are you sure you want to retry this transfer?')) {
        try {
            await transfersStore.retryTransfer(executionId);
            // Show success toast
            if (typeof window !== 'undefined' && (window as any).showToast) {
                (window as any).showToast('Transfer retry initiated successfully', 'success');
            }
            await loadTransfers();
        } catch (err) {
            // Show error toast
            if (typeof window !== 'undefined' && (window as any).showToast) {
                const message = err instanceof Error ? err.message : 'Failed to retry transfer';
                (window as any).showToast(message, 'error');
            }
        }
    }
};

const deleteTransfer = async (executionId: number) => {
    if (confirm('Are you sure you want to delete this transfer? This will not delete the transferred files.')) {
        try {
            await transfersStore.deleteTransfer(executionId);
            // Show success toast
            if (typeof window !== 'undefined' && (window as any).showToast) {
                (window as any).showToast('Transfer deleted successfully', 'success');
            }
            await loadTransfers();
        } catch (err) {
            // Show error toast
            if (typeof window !== 'undefined' && (window as any).showToast) {
                const message = err instanceof Error ? err.message : 'Failed to delete transfer';
                (window as any).showToast(message, 'error');
            }
        }
    }
};

onMounted(async () => {
    await loadTransfers();
    // Start polling for each running transfer
    transfersStore.runningTransfers.forEach(transfer => {
        transfersStore.startPolling(transfer.executionId);
    });
});

onUnmounted(() => {
    // Clean up all polling intervals
    transfersStore.stopAllPolling();
});
</script>

<style scoped>
.transfer-history {
    padding: 1.5rem;
}

h1 {
    margin-bottom: 1.5rem;
    color: #2c3e50;
}

.no-transfers {
    text-align: center;
    padding: 3rem;
    color: #666;
}

.no-transfers a {
    color: #42b983;
    text-decoration: none;
}

.no-transfers a:hover {
    text-decoration: underline;
}

.transfers-table {
    width: 100%;
    border-collapse: collapse;
    background: white;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    overflow: hidden;
}

.transfers-table thead {
    background: #f5f5f5;
}

.transfers-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 600;
    color: #2c3e50;
    border-bottom: 2px solid #e0e0e0;
}

.transfers-table td {
    padding: 1rem;
    border-bottom: 1px solid #f0f0f0;
}

.transfer-row {
    cursor: pointer;
    transition: background-color 0.2s;
}

.transfer-row:hover {
    background-color: #f9f9f9;
}

.path-cell {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.actions-cell {
    display: flex;
    gap: 0.5rem;
}

.btn-retry,
.btn-delete {
    padding: 0.4rem 0.8rem;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.875rem;
    transition: opacity 0.2s;
}

.btn-retry {
    background: #42b983;
    color: white;
}

.btn-retry:hover {
    opacity: 0.8;
}

.btn-delete {
    background: #e74c3c;
    color: white;
}

.btn-delete:hover {
    opacity: 0.8;
}

.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 1rem;
    margin-top: 1.5rem;
}

.btn-page {
    padding: 0.5rem 1rem;
    border: 1px solid #ddd;
    background: white;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-page:hover:not(:disabled) {
    background: #f5f5f5;
    border-color: #42b983;
}

.btn-page:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.page-info {
    color: #666;
    font-size: 0.875rem;
}

@media (max-width: 768px) {
    .transfer-history {
        padding: 1rem;
    }

    h1 {
        font-size: 1.5rem;
    }

    /* Switch to card layout on mobile for better readability */
    .transfers-table {
        display: block;
        box-shadow: none;
        background: transparent;
    }

    .transfers-table thead {
        display: none;
    }

    .transfers-table tbody {
        display: block;
    }

    .transfer-row {
        display: block;
        background: white;
        margin-bottom: 1rem;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        padding: 1rem;
        min-height: 44px;
    }

    .transfer-row:hover {
        background-color: white;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15);
    }

    .transfers-table td {
        display: block;
        padding: 0.5rem 0;
        border: none;
        text-align: left;
    }

    .transfers-table td::before {
        content: attr(data-label);
        font-weight: 600;
        color: var(--color-text-secondary, #6b7280);
        display: inline-block;
        width: 100px;
        margin-right: 0.5rem;
    }

    .path-cell {
        max-width: 100%;
        word-break: break-all;
    }

    .actions-cell {
        display: flex;
        gap: 0.5rem;
        padding-top: 1rem;
        border-top: 1px solid #f0f0f0;
        margin-top: 0.5rem;
    }

    .actions-cell::before {
        display: none;
    }

    .btn-retry,
    .btn-delete {
        flex: 1;
        min-height: 44px;
        font-size: 1rem;
    }

    .pagination {
        flex-direction: column;
        gap: 0.5rem;
    }

    .btn-page {
        width: 100%;
        min-height: 44px;
    }
}
</style>
