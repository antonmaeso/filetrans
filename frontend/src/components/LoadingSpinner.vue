<script setup lang="ts">
interface LoadingSpinnerProps {
    size?: 'small' | 'medium' | 'large';
    message?: string;
}

const props = withDefaults(defineProps<LoadingSpinnerProps>(), {
    size: 'medium',
    message: undefined,
});

const sizeClasses = {
    small: 'spinner-small',
    medium: 'spinner-medium',
    large: 'spinner-large',
};
</script>

<template>
    <div class="loading-spinner-container">
        <div :class="['spinner', sizeClasses[props.size]]"></div>
        <p v-if="props.message" class="spinner-message">{{ props.message }}</p>
    </div>
</template>

<style scoped>
.loading-spinner-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
}

.spinner {
    border-radius: 50%;
    border-style: solid;
    border-color: #3b82f6 transparent #3b82f6 transparent;
    animation: spin 1.2s linear infinite;
}

.spinner-small {
    width: 20px;
    height: 20px;
    border-width: 2px;
}

.spinner-medium {
    width: 40px;
    height: 40px;
    border-width: 4px;
}

.spinner-large {
    width: 60px;
    height: 60px;
    border-width: 6px;
}

.spinner-message {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
    text-align: center;
}

@keyframes spin {
    0% {
        transform: rotate(0deg);
    }
    100% {
        transform: rotate(360deg);
    }
}
</style>
