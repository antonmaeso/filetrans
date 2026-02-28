<script setup lang="ts">
import { computed } from 'vue';
import type { TransferStatus } from '@/api/types';

interface Props {
    status: TransferStatus;
}

const props = defineProps<Props>();

/**
 * Get the CSS class for the badge based on status
 */
const badgeClass = computed(() => {
    switch (props.status) {
        case 'COMPLETED':
            return 'badge-green';
        case 'STARTED':
        case 'STARTING':
            return 'badge-blue';
        case 'FAILED':
            return 'badge-red';
        case 'STOPPED':
        case 'STOPPING':
            return 'badge-orange';
        case 'ABANDONED':
        case 'UNKNOWN':
        default:
            return 'badge-gray';
    }
});

/**
 * Get display text for the status
 */
const displayText = computed(() => {
    return props.status.charAt(0) + props.status.slice(1).toLowerCase();
});
</script>

<template>
    <span class="status-badge" :class="badgeClass">
        {{ displayText }}
    </span>
</template>

<style scoped>
.status-badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.875rem;
    font-weight: 500;
    text-align: center;
    white-space: nowrap;
}

.badge-green {
    background-color: #d1fae5;
    color: #065f46;
}

.badge-blue {
    background-color: #dbeafe;
    color: #1e40af;
}

.badge-red {
    background-color: #fee2e2;
    color: #991b1b;
}

.badge-orange {
    background-color: #fed7aa;
    color: #9a3412;
}

.badge-gray {
    background-color: #e5e7eb;
    color: #374151;
}
</style>
