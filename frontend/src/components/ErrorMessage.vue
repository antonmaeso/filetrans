<script setup lang="ts">
interface ErrorMessageProps {
    message: string;
    retryable?: boolean;
}

defineProps<ErrorMessageProps>();

const emit = defineEmits<{
    retry: [];
}>();

const handleRetry = () => {
    emit('retry');
};
</script>

<template>
    <div class="error-message">
        <div class="error-icon">⚠️</div>
        <div class="error-content">
            <p class="error-text">{{ message }}</p>
            <button v-if="retryable" class="retry-button" @click="handleRetry">Retry</button>
        </div>
    </div>
</template>

<style scoped>
.error-message {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
    background-color: #fee;
    border: 1px solid #fcc;
    border-radius: 8px;
    color: #c33;
}

.error-icon {
    font-size: 24px;
    line-height: 1;
    flex-shrink: 0;
}

.error-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.error-text {
    margin: 0;
    font-size: 14px;
    line-height: 1.5;
    color: #c33;
}

.retry-button {
    align-self: flex-start;
    padding: 8px 16px;
    background-color: #c33;
    color: white;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 0.2s;
}

.retry-button:hover {
    background-color: #a22;
}

.retry-button:active {
    background-color: #911;
}

.retry-button:focus {
    outline: 2px solid #c33;
    outline-offset: 2px;
}
</style>
