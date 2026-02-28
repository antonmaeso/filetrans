<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useTransferStore } from '@/stores/transfers';
import TransferStatusBadge from '@/components/TransferStatusBadge.vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import ErrorMessage from '@/components/ErrorMessage.vue';
import { formatDate } from '@/utils/formatters';

const router = useRouter();
const transferStore = useTransferStore();

const pollingInterval = ref<number | null>(null);

/**
 * System statistics computed from store data
 */
const statistics = computed(() => {
    const total = transferStore.pagination.totalElements;
    const running = transferStore.runningTransfers.length;
    const completed = transferStore.completedTransfers.length;
    const failed = transferStore.failedTransfers.length;

    return {
        totalTransfers: total,
        runningTransfers: running,
        completedTransfers: completed,
        failedTransfers: failed,
    };
});

/**
 * Get the 10 most recent transfers
 */
const recentTransfers = computed(() => {
    return transferStore.transfers.slice(0, 10);
});

/**
 * Load initial data
 */
async function loadData() {
    try {
        await transferStore.loadTransfers(0, 10, 'startTime,desc', true);
    } catch (err) {
        console.error('[Dashboard] Failed to load transfers:', err);
    }
}

/**
 * Navigate to transfer details
 */
function viewTransferDetails(executionId: number) {
    router.push(`/transfers/${executionId}`);
}

/**
 * Retry loading data
 */
function retryLoad() {
    transferStore.clearError();
    loadData();
}

/**
 * Start polling for running transfers
 */
function startPolling() {
    pollingInterval.value = window.setInterval(async () => {
        // Only poll if there are running transfers
        if (transferStore.runningTransfers.length > 0) {
            try {
                await transferStore.loadTransfers(0, 10, 'startTime,desc', true);
            } catch (err) {
                console.error('[Dashboard] Polling error:', err);
            }
        }
    }, 5000); // Poll every 5 seconds
}

/**
 * Stop polling
 */
function stopPolling() {
    if (pollingInterval.value !== null) {
        window.clearInterval(pollingInterval.value);
        pollingInterval.value = null;
    }
}

// Lifecycle hooks
onMounted(() => {
    loadData();
    startPolling();
});

onUnmounted(() => {
    stopPolling();
});
</script>

<template>
    <div class="dashboard">
        <div class="dashboard-header">
            <h1>Dashboard</h1>
            <p class="subtitle">Overview of recent transfer activity</p>
        </div>

        <!-- System Statistics -->
        <div class="statistics-grid">
            <div class="stat-card">
                <div class="stat-value">{{ statistics.totalTransfers }}</div>
                <div class="stat-label">Total Transfers</div>
            </div>
            <div class="stat-card stat-running">
                <div class="stat-value">{{ statistics.runningTransfers }}</div>
                <div class="stat-label">Running</div>
            </div>
            <div class="stat-card stat-completed">
                <div class="stat-value">{{ statistics.completedTransfers }}</div>
                <div class="stat-label">Completed</div>
            </div>
            <div class="stat-card stat-failed">
                <div class="stat-value">{{ statistics.failedTransfers }}</div>
                <div class="stat-label">Failed</div>
            </div>
        </div>

        <!-- Loading State -->
        <div v-if="transferStore.loading && recentTransfers.length === 0" class="loading-container">
            <LoadingSpinner size="large" message="Loading transfers..." />
        </div>

        <!-- Error State -->
        <ErrorMessage
            v-else-if="transferStore.error && recentTransfers.length === 0"
            :message="transferStore.error"
            :retryable="true"
            @retry="retryLoad"
        />

        <!-- Recent Transfers Table -->
        <div v-else class="transfers-section">
            <h2>Recent Transfers</h2>

            <div v-if="recentTransfers.length === 0" class="empty-state">
                <p>No transfers found. Create your first transfer to get started.</p>
                <button class="create-button" @click="router.push('/transfers/new')">
                    Create Transfer
                </button>
            </div>

            <div v-else class="table-container">
                <table class="transfers-table">
                    <thead>
                        <tr>
                            <th>Execution ID</th>
                            <th>Start Time</th>
                            <th>Status</th>
                            <th>Files</th>
                            <th>Source</th>
                            <th>Target</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr
                            v-for="transfer in recentTransfers"
                            :key="transfer.executionId"
                            class="transfer-row"
                            @click="viewTransferDetails(transfer.executionId)"
                        >
                            <td class="execution-id">{{ transfer.executionId }}</td>
                            <td>{{ formatDate(transfer.startTime) }}</td>
                            <td>
                                <TransferStatusBadge :status="transfer.status" />
                            </td>
                            <td class="file-count">{{ transfer.fileCount }}</td>
                            <td class="path-cell">{{ transfer.sourcePath }}</td>
                            <td class="path-cell">{{ transfer.targetPath }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="recentTransfers.length > 0" class="view-all-container">
                <button class="view-all-button" @click="router.push('/transfers')">
                    View All Transfers
                </button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.dashboard {
    max-width: 1400px;
    margin: 0 auto;
    padding: 24px;
}

.dashboard-header {
    margin-bottom: 32px;
}

.dashboard-header h1 {
    margin: 0 0 8px 0;
    font-size: 32px;
    font-weight: 600;
    color: #111827;
}

.subtitle {
    margin: 0;
    font-size: 16px;
    color: #6b7280;
}

/* Statistics Grid */
.statistics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 32px;
}

.stat-card {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 24px;
    text-align: center;
    transition: box-shadow 0.2s;
}

.stat-card:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.stat-value {
    font-size: 36px;
    font-weight: 700;
    color: #111827;
    margin-bottom: 8px;
}

.stat-label {
    font-size: 14px;
    color: #6b7280;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.stat-running .stat-value {
    color: #1e40af;
}

.stat-completed .stat-value {
    color: #065f46;
}

.stat-failed .stat-value {
    color: #991b1b;
}

/* Loading and Error States */
.loading-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 300px;
}

/* Transfers Section */
.transfers-section {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 24px;
}

.transfers-section h2 {
    margin: 0 0 20px 0;
    font-size: 20px;
    font-weight: 600;
    color: #111827;
}

/* Empty State */
.empty-state {
    text-align: center;
    padding: 48px 24px;
}

.empty-state p {
    margin: 0 0 20px 0;
    font-size: 16px;
    color: #6b7280;
}

.create-button {
    padding: 12px 24px;
    background-color: #3b82f6;
    color: white;
    border: none;
    border-radius: 6px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 0.2s;
}

.create-button:hover {
    background-color: #2563eb;
}

.create-button:active {
    background-color: #1d4ed8;
}

/* Table */
.table-container {
    overflow-x: auto;
}

.transfers-table {
    width: 100%;
    border-collapse: collapse;
}

.transfers-table th {
    text-align: left;
    padding: 12px;
    background-color: #f9fafb;
    border-bottom: 2px solid #e5e7eb;
    font-size: 14px;
    font-weight: 600;
    color: #374151;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.transfers-table td {
    padding: 16px 12px;
    border-bottom: 1px solid #e5e7eb;
    font-size: 14px;
    color: #111827;
}

.transfer-row {
    cursor: pointer;
    transition: background-color 0.2s;
}

.transfer-row:hover {
    background-color: #f9fafb;
}

.execution-id {
    font-weight: 600;
    color: #3b82f6;
}

.file-count {
    font-weight: 500;
}

.path-cell {
    max-width: 250px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #6b7280;
    font-size: 13px;
}

/* View All Button */
.view-all-container {
    margin-top: 20px;
    text-align: center;
}

.view-all-button {
    padding: 10px 20px;
    background-color: white;
    color: #3b82f6;
    border: 1px solid #3b82f6;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.view-all-button:hover {
    background-color: #3b82f6;
    color: white;
}

/* Responsive Design */
@media (max-width: 768px) {
    .dashboard {
        padding: 16px;
    }

    .dashboard-header h1 {
        font-size: 24px;
    }

    .statistics-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
    }

    .stat-card {
        padding: 16px;
    }

    .stat-value {
        font-size: 28px;
    }

    .table-container {
        overflow-x: scroll;
    }

    .transfers-table {
        min-width: 800px;
    }

    .path-cell {
        max-width: 150px;
    }
}

@media (max-width: 480px) {
    .statistics-grid {
        grid-template-columns: 1fr;
    }
}
</style>
