<script setup lang="ts">
import { ref } from 'vue';
import SearchBar from './SearchBar.vue';

const searchQuery = ref('');

const handleSearch = (query: string) => {
    searchQuery.value = query;
    console.log('Search query:', query);
};
</script>

<template>
    <div style="padding: 20px; max-width: 600px">
        <h2>SearchBar Component Example</h2>

        <div style="margin: 20px 0">
            <SearchBar placeholder="Search files..." :debounce-ms="500" @search="handleSearch" />
        </div>

        <div
            v-if="searchQuery"
            style="margin-top: 20px; padding: 10px; background: #f3f4f6; border-radius: 4px"
        >
            <strong>Current search query:</strong> {{ searchQuery }}
        </div>

        <div style="margin-top: 20px; color: #6b7280; font-size: 14px">
            <p>Features:</p>
            <ul>
                <li>Debounced input (500ms default)</li>
                <li>Clear button appears when text is entered</li>
                <li>Emits 'search' event with query string</li>
                <li>Customizable placeholder and debounce time</li>
                <li>Responsive design with mobile support</li>
            </ul>
        </div>
    </div>
</template>
