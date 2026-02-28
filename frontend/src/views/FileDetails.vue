<template>
    <div class="file-details">
        <div class="header">
            <button @click="goBack" class="btn-back">← Back</button>
            <h1>File Details</h1>
        </div>

        <LoadingSpinner v-if="filesStore.loading" message="Loading file metadata..." />
        <ErrorMessage 
            v-else-if="filesStore.error" 
            :message="filesStore.error"
            :retryable="true"
            @retry="loadMetadata"
        />

        <div v-else-if="metadata" class="details-container">
            <!-- Image Preview -->
            <div v-if="isImage" class="image-preview">
                <img :src="getImageUrl()" :alt="metadata.filename" />
            </div>

            <!-- Basic File Information -->
            <section class="info-section">
                <h2>File Information</h2>
                <div class="info-grid">
                    <div class="info-item">
                        <span class="label">Filename:</span>
                        <span class="value">{{ metadata.filename }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">Path:</span>
                        <span class="value path">{{ metadata.path }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">Size:</span>
                        <span class="value">{{ formatFileSize(metadata.size) }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">Transfer Date:</span>
                        <span class="value">{{ formatDate(metadata.transferDate) }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">Source Path:</span>
                        <span class="value path">{{ metadata.sourcePath }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">Hash ({{ metadata.hashAlgorithm }}):</span>
                        <span class="value hash">{{ metadata.hash }}</span>
                    </div>
                </div>
            </section>

            <!-- EXIF Data -->
            <section v-if="metadata.exif" class="info-section">
                <h2>EXIF Data</h2>
                <div class="info-grid">
                    <div v-if="metadata.exif.cameraModel" class="info-item">
                        <span class="label">Camera Model:</span>
                        <span class="value">{{ metadata.exif.cameraModel }}</span>
                    </div>
                    <div v-if="metadata.exif.captureDate" class="info-item">
                        <span class="label">Capture Date:</span>
                        <span class="value">{{ formatDate(metadata.exif.captureDate) }}</span>
                    </div>
                    <div v-if="metadata.exif.focalLength" class="info-item">
                        <span class="label">Focal Length:</span>
                        <span class="value">{{ metadata.exif.focalLength }}</span>
                    </div>
                    <div v-if="metadata.exif.aperture" class="info-item">
                        <span class="label">Aperture:</span>
                        <span class="value">{{ metadata.exif.aperture }}</span>
                    </div>
                    <div v-if="metadata.exif.iso" class="info-item">
                        <span class="label">ISO:</span>
                        <span class="value">{{ metadata.exif.iso }}</span>
                    </div>
                    <div v-if="metadata.exif.shutterSpeed" class="info-item">
                        <span class="label">Shutter Speed:</span>
                        <span class="value">{{ metadata.exif.shutterSpeed }}</span>
                    </div>
                    <div v-if="metadata.exif.gpsLatitude && metadata.exif.gpsLongitude" class="info-item full-width">
                        <span class="label">GPS Coordinates:</span>
                        <span class="value">
                            {{ metadata.exif.gpsLatitude }}, {{ metadata.exif.gpsLongitude }}
                            <a 
                                :href="`https://www.google.com/maps?q=${metadata.exif.gpsLatitude},${metadata.exif.gpsLongitude}`"
                                target="_blank"
                                class="map-link"
                            >
                                View on Map
                            </a>
                        </span>
                    </div>
                </div>
            </section>

            <!-- AI Analysis -->
            <section v-if="metadata.aiAnalysis" class="info-section">
                <h2>AI Analysis</h2>
                <div class="ai-content">
                    <div class="info-item full-width">
                        <span class="label">Description:</span>
                        <p class="value description">{{ metadata.aiAnalysis.description }}</p>
                    </div>
                    <div class="info-item full-width">
                        <span class="label">Tags:</span>
                        <div class="tags">
                            <span v-for="tag in metadata.aiAnalysis.tags" :key="tag" class="tag">
                                {{ tag }}
                            </span>
                        </div>
                    </div>
                    <div class="info-item">
                        <span class="label">Confidence Score:</span>
                        <span class="value">{{ (metadata.aiAnalysis.confidence * 100).toFixed(1) }}%</span>
                    </div>
                </div>
            </section>

            <!-- Metadata Not Available -->
            <div v-if="!metadata.exif && !metadata.aiAnalysis" class="no-metadata">
                <p>Additional metadata is being processed...</p>
                <LoadingSpinner size="small" />
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useFileStore } from '../stores/files';
import { formatDate, formatFileSize } from '../utils/formatters';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import ErrorMessage from '../components/ErrorMessage.vue';

const router = useRouter();
const route = useRoute();
const filesStore = useFileStore();

const id = computed(() => route.params.id as string);
const metadata = computed(() => filesStore.currentFile);
const pollingInterval = ref<number | null>(null);

const isImage = computed(() => {
    if (!metadata.value) return false;
    const ext = metadata.value.filename.split('.').pop()?.toLowerCase();
    return ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '');
});

const loadMetadata = async () => {
    await filesStore.loadFileMetadata(id.value);
};

const getImageUrl = () => {
    // Construct the full image URL - adjust based on your API
    return `/api/files/${id.value}/image`;
};

const goBack = () => {
    router.back();
};

const startPolling = () => {
    // Poll every 2 seconds if metadata is incomplete
    pollingInterval.value = window.setInterval(async () => {
        if (metadata.value && (!metadata.value.exif && !metadata.value.aiAnalysis)) {
            await loadMetadata();
        } else {
            stopPolling();
        }
    }, 2000);
};

const stopPolling = () => {
    if (pollingInterval.value) {
        clearInterval(pollingInterval.value);
        pollingInterval.value = null;
    }
};

onMounted(async () => {
    await loadMetadata();
    
    // Start polling if metadata is incomplete
    if (metadata.value && (!metadata.value.exif && !metadata.value.aiAnalysis)) {
        startPolling();
    }
});

onUnmounted(() => {
    stopPolling();
});
</script>

<style scoped>
.file-details {
    padding: 1.5rem;
    max-width: 1200px;
    margin: 0 auto;
}

.header {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 1.5rem;
}

.btn-back {
    padding: 0.5rem 1rem;
    border: 1px solid #ddd;
    background: white;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-back:hover {
    background: #f5f5f5;
    border-color: #42b983;
}

h1 {
    color: #2c3e50;
    margin: 0;
}

.details-container {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.image-preview {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    padding: 1rem;
    text-align: center;
}

.image-preview img {
    max-width: 100%;
    max-height: 600px;
    border-radius: 4px;
}

.info-section {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    padding: 1.5rem;
}

.info-section h2 {
    margin: 0 0 1rem 0;
    color: #2c3e50;
    font-size: 1.25rem;
    border-bottom: 2px solid #42b983;
    padding-bottom: 0.5rem;
}

.info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 1rem;
}

.info-item {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.info-item.full-width {
    grid-column: 1 / -1;
}

.label {
    font-weight: 600;
    color: #666;
    font-size: 0.875rem;
}

.value {
    color: #2c3e50;
    word-break: break-word;
}

.value.path {
    font-family: monospace;
    font-size: 0.875rem;
    background: #f5f5f5;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
}

.value.hash {
    font-family: monospace;
    font-size: 0.75rem;
    background: #f5f5f5;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
}

.value.description {
    margin: 0;
    line-height: 1.6;
}

.map-link {
    margin-left: 0.5rem;
    color: #42b983;
    text-decoration: none;
}

.map-link:hover {
    text-decoration: underline;
}

.ai-content {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.tags {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
}

.tag {
    background: #42b983;
    color: white;
    padding: 0.25rem 0.75rem;
    border-radius: 16px;
    font-size: 0.875rem;
}

.no-metadata {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    padding: 2rem;
    text-align: center;
    color: #666;
}

@media (max-width: 768px) {
    .file-details {
        padding: 1rem;
    }

    .header {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.75rem;
    }

    .btn-back {
        width: 100%;
        min-height: 44px;
        justify-content: center;
        display: flex;
        align-items: center;
    }

    h1 {
        font-size: 1.5rem;
    }

    .info-section {
        padding: 1rem;
    }

    .info-section h2 {
        font-size: 1.125rem;
    }

    .info-grid {
        grid-template-columns: 1fr;
        gap: 0.75rem;
    }

    .image-preview {
        padding: 0.5rem;
    }

    .image-preview img {
        max-height: 300px;
    }

    .value.path,
    .value.hash {
        font-size: 0.75rem;
        word-break: break-all;
    }

    .tags {
        gap: 0.375rem;
    }

    .tag {
        font-size: 0.8125rem;
    }
}

/* Extra small screens */
@media (max-width: 480px) {
    .file-details {
        padding: 0.5rem;
    }

    .info-section {
        padding: 0.75rem;
    }

    .image-preview img {
        max-height: 250px;
    }
}
</style>
