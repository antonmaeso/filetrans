<script setup lang="ts">
import { ref, watch } from 'vue';

interface Props {
    placeholder?: string;
    debounceMs?: number;
}

const props = withDefaults(defineProps<Props>(), {
    placeholder: 'Search...',
    debounceMs: 500,
});

const emit = defineEmits<{
    search: [query: string];
}>();

const query = ref('');
let debounceTimeout: number | null = null;

watch(query, newQuery => {
    if (debounceTimeout !== null) {
        clearTimeout(debounceTimeout);
    }

    debounceTimeout = setTimeout(() => {
        emit('search', newQuery);
    }, props.debounceMs);
});

const handleClear = () => {
    query.value = '';
};
</script>

<template>
    <div class="search-bar">
        <div class="search-input-wrapper">
            <svg
                class="search-icon"
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
            >
                <circle cx="11" cy="11" r="8"></circle>
                <path d="m21 21-4.35-4.35"></path>
            </svg>

            <input v-model="query" type="text" class="search-input" :placeholder="placeholder" />

            <button
                v-if="query"
                class="clear-button"
                type="button"
                aria-label="Clear search"
                @click="handleClear"
            >
                <svg
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                >
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>
        </div>
    </div>
</template>

<style scoped>
.search-bar {
    width: 100%;
}

.search-input-wrapper {
    position: relative;
    display: flex;
    align-items: center;
}

.search-icon {
    position: absolute;
    left: 12px;
    width: 20px;
    height: 20px;
    color: #6b7280;
    pointer-events: none;
}

.search-input {
    width: 100%;
    padding: 10px 40px 10px 40px;
    font-size: 14px;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    outline: none;
    transition:
        border-color 0.2s,
        box-shadow 0.2s;
}

.search-input:focus {
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.clear-button {
    position: absolute;
    right: 8px;
    width: 24px;
    height: 24px;
    padding: 4px;
    background: none;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    color: #6b7280;
    transition:
        background-color 0.2s,
        color 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
}

.clear-button:hover {
    background-color: #f3f4f6;
    color: #374151;
}

.clear-button svg {
    width: 16px;
    height: 16px;
}

/* Mobile responsive */
@media (max-width: 768px) {
    .search-input {
        font-size: 16px; /* Prevents zoom on iOS */
    }
}
</style>
