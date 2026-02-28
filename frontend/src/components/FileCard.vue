<script setup lang="ts">
import type { FileListItem } from '@/api/types';
import { formatFileSize, formatDate } from '@/utils/formatters';

interface Props {
    file: FileListItem;
}

const props = defineProps<Props>();

/**
 * Emit click event when card is clicked
 */
const emit = defineEmits<{
    click: [];
}>();

/**
 * Handle card click
 */
const handleClick = () => {
    emit('click');
};
</script>

<template>
    <div class="file-card" @click="handleClick">
        <div class="file-card-thumbnail">
            <img
                v-if="props.file.thumbnailUrl"
                :src="props.file.thumbnailUrl"
                :alt="props.file.filename"
                class="thumbnail-image"
            />
            <div v-else class="thumbnail-placeholder">
                <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    class="file-icon"
                >
                    <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"
                    />
                </svg>
            </div>
        </div>
        <div class="file-card-content">
            <h3 class="file-card-filename" :title="props.file.filename">
                {{ props.file.filename }}
            </h3>
            <div class="file-card-details">
                <span class="file-card-size">{{ formatFileSize(props.file.size) }}</span>
                <span class="file-card-separator">•</span>
                <span class="file-card-date">{{ formatDate(props.file.transferDate) }}</span>
            </div>
        </div>
    </div>
</template>

<style scoped>
.file-card {
    display: flex;
    flex-direction: column;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.2s ease;
}

.file-card:hover {
    box-shadow:
        0 4px 6px -1px rgba(0, 0, 0, 0.1),
        0 2px 4px -1px rgba(0, 0, 0, 0.06);
    border-color: #3b82f6;
    transform: translateY(-2px);
}

.file-card-thumbnail {
    width: 100%;
    aspect-ratio: 1;
    background-color: #f3f4f6;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
}

.thumbnail-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.thumbnail-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: #9ca3af;
}

.file-icon {
    width: 48px;
    height: 48px;
}

.file-card-content {
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.file-card-filename {
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    color: #111827;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.file-card-details {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #6b7280;
}

.file-card-size,
.file-card-date {
    white-space: nowrap;
}

.file-card-separator {
    color: #d1d5db;
}

/* Responsive adjustments */
@media (max-width: 768px) {
    .file-card-content {
        padding: 10px;
    }

    .file-card-filename {
        font-size: 13px;
    }

    .file-card-details {
        font-size: 11px;
    }

    .file-icon {
        width: 40px;
        height: 40px;
    }
}

/* Touch-friendly sizing for mobile */
@media (max-width: 768px) {
    .file-card {
        min-height: 44px;
    }
}
</style>
