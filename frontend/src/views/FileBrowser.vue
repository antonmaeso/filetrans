<template>
    <div class="file-browser">
        <div class="header">
            <h1>File Browser</h1>
            
            <!-- Search Bar -->
            <div class="search-section">
                <SearchBar 
                    placeholder="Search files by name, description, or tags..."
                    @search="handleSearch"
                />
                
                <!-- Filters -->
                <div class="filters">
                    <div class="filter-group">
                        <label>Date Range:</label>
                        <input 
                            type="date" 
                            v-model="filters.dateFrom"
                            @change="applyFilters"
                            class="date-input"
                        />
                        <span>to</span>
                        <input 
                            type="date" 
                            v-model="filters.dateTo"
                            @change="applyFilters"
                            class="date-input"
                        />
                    </div>
                    
                    <div class="filter-group" v-if="availableTags.length > 0">
                        <label>Tags:</label>
                        <select 
                            v-model="filters.selectedTags"
                            multiple
                            @change="applyFilters"
                            class="tag-select"
                        >
                            <option v-for="tag in availableTags" :key="tag" :value="tag">
                                {{ tag }}
                            </option>
                        </select>
                    </div>
                    
                    <button 
                        v-if="isSearchActive"
                        @click="clearSearch"
                        class="btn-clear"
                    >
                        Clear Search
                    </button>
                </div>
            </div>
        </div>

        <LoadingSpinner v-if="filesStore.loading" message="Loading files..." />
        <ErrorMessage 
            v-else-if="filesStore.error" 
            :message="filesStore.error"
            :retryable="true"
            @retry="loadFiles"
        />

        <div v-else>
            <!-- Search Results -->
            <div v-if="isSearchActive">
                <h2 class="results-header">
                    Search Results ({{ filesStore.searchResults.length }} files)
                </h2>
                
                <div v-if="filesStore.searchResults.length === 0" class="no-results">
                    <p>No files found matching your search criteria.</p>
                    <p>Try adjusting your search terms or filters.</p>
                </div>
                
                <div v-else class="search-results-grid">
                    <FileCard 
                        v-for="file in filesStore.searchResults" 
                        :key="file.id"
                        :file="file"
                        @click="navigateToDetails(file.id)"
                    />
                </div>
            </div>

            <!-- File Tree (when not searching) -->
            <div v-else>
                <div v-if="Object.keys(filesStore.fileTree).length === 0" class="no-files">
                    <p>No files found. <router-link to="/transfers/new">Transfer some files</router-link></p>
                </div>

                <div v-else class="file-tree">
                    <div v-for="(dates, year) in filesStore.fileTree" :key="year" class="year-folder">
                        <div class="folder-header" @click="toggleYear(year)">
                            <span class="folder-icon">{{ expandedYears.has(year) ? '📂' : '📁' }}</span>
                            <span class="folder-name">{{ year }}</span>
                            <span class="folder-count">({{ getYearFileCount(dates) }} files)</span>
                        </div>

                        <div v-if="expandedYears.has(year)" class="date-folders">
                            <div v-for="(files, date) in dates" :key="date" class="date-folder">
                                <div class="folder-header date-header" @click="toggleDate(year, date)">
                                    <span class="folder-icon">{{ expandedDates.has(`${year}-${date}`) ? '📂' : '📁' }}</span>
                                    <span class="folder-name">{{ date }}</span>
                                    <span class="folder-count">({{ files.length }} files)</span>
                                </div>

                                <div v-if="expandedDates.has(`${year}-${date}`)" class="files-grid">
                                    <FileCard 
                                        v-for="file in files" 
                                        :key="file.id"
                                        :file="file"
                                        @click="navigateToDetails(file.id)"
                                    />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="pagination" v-if="currentPagination.totalPages > 1">
                <button 
                    @click="goToPage(currentPagination.page - 1)"
                    :disabled="currentPagination.page === 0"
                    class="btn-page"
                >
                    Previous
                </button>
                <span class="page-info">
                    Page {{ currentPagination.page + 1 }} of {{ currentPagination.totalPages }}
                </span>
                <button 
                    @click="goToPage(currentPagination.page + 1)"
                    :disabled="currentPagination.page >= currentPagination.totalPages - 1"
                    class="btn-page"
                >
                    Next
                </button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useFileStore } from '../stores/files';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import ErrorMessage from '../components/ErrorMessage.vue';
import FileCard from '../components/FileCard.vue';
import SearchBar from '../components/SearchBar.vue';
import type { FileListItem } from '../api/types';

const router = useRouter();
const filesStore = useFileStore();

const expandedYears = ref<Set<string>>(new Set());
const expandedDates = ref<Set<string>>(new Set());
const searchQuery = ref('');
const filters = ref({
    dateFrom: '',
    dateTo: '',
    selectedTags: [] as string[]
});

// Mock available tags - in a real app, this would come from the API
const availableTags = ref<string[]>([
    'landscape', 'portrait', 'nature', 'urban', 'indoor', 'outdoor'
]);

const isSearchActive = computed(() => {
    return searchQuery.value !== '' || 
           filters.value.dateFrom !== '' || 
           filters.value.dateTo !== '' ||
           filters.value.selectedTags.length > 0;
});

const currentPagination = computed(() => {
    return isSearchActive.value 
        ? { page: 0, totalPages: 1, size: 50, totalElements: filesStore.searchResults.length }
        : filesStore.pagination;
});

const loadFiles = async () => {
    await filesStore.loadFiles();
};

const goToPage = async (page: number) => {
    if (isSearchActive.value) {
        await performSearch(page);
    } else {
        await filesStore.loadFiles(page);
    }
};

const handleSearch = (query: string) => {
    searchQuery.value = query;
    performSearch();
};

const applyFilters = () => {
    if (isSearchActive.value) {
        performSearch();
    }
};

const performSearch = async (page: number = 0) => {
    await filesStore.searchFiles({
        q: searchQuery.value || undefined,
        tags: filters.value.selectedTags.length > 0 ? filters.value.selectedTags : undefined,
        dateFrom: filters.value.dateFrom || undefined,
        dateTo: filters.value.dateTo || undefined,
        page,
        size: 50
    });
};

const clearSearch = async () => {
    searchQuery.value = '';
    filters.value = {
        dateFrom: '',
        dateTo: '',
        selectedTags: []
    };
    await loadFiles();
};

const toggleYear = (year: string) => {
    if (expandedYears.value.has(year)) {
        expandedYears.value.delete(year);
        // Also collapse all dates in this year
        Object.keys(filesStore.fileTree[year] || {}).forEach(date => {
            expandedDates.value.delete(`${year}-${date}`);
        });
    } else {
        expandedYears.value.add(year);
    }
};

const toggleDate = (year: string, date: string) => {
    const key = `${year}-${date}`;
    if (expandedDates.value.has(key)) {
        expandedDates.value.delete(key);
    } else {
        expandedDates.value.add(key);
    }
};

const getYearFileCount = (dates: Record<string, FileListItem[]>): number => {
    return Object.values(dates).reduce((sum, files) => sum + files.length, 0);
};

const navigateToDetails = (fileId: string) => {
    router.push(`/files/${fileId}`);
};

onMounted(async () => {
    await loadFiles();
});
</script>

<style scoped>
.file-browser {
    padding: 1.5rem;
}

.header {
    margin-bottom: 1.5rem;
}

h1 {
    margin-bottom: 1rem;
    color: #2c3e50;
}

.search-section {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    margin-bottom: 1.5rem;
}

.filters {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    margin-top: 1rem;
    align-items: flex-end;
}

.filter-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.filter-group label {
    font-weight: 600;
    font-size: 0.875rem;
    color: #666;
}

.date-input {
    padding: 0.5rem;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 0.875rem;
}

.tag-select {
    padding: 0.5rem;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 0.875rem;
    min-width: 200px;
}

.btn-clear {
    padding: 0.5rem 1rem;
    background: #e74c3c;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    transition: opacity 0.2s;
    align-self: flex-end;
}

.btn-clear:hover {
    opacity: 0.8;
}

.results-header {
    color: #2c3e50;
    margin-bottom: 1rem;
    font-size: 1.25rem;
}

.no-results {
    text-align: center;
    padding: 3rem;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    color: #666;
}

.search-results-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 1rem;
    background: white;
    padding: 1rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.no-files {
    text-align: center;
    padding: 3rem;
    color: #666;
}

.no-files a {
    color: #42b983;
    text-decoration: none;
}

.no-files a:hover {
    text-decoration: underline;
}

.file-tree {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    overflow: hidden;
}

.year-folder {
    border-bottom: 1px solid #e0e0e0;
}

.year-folder:last-child {
    border-bottom: none;
}

.folder-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 1rem;
    cursor: pointer;
    transition: background-color 0.2s;
    user-select: none;
}

.folder-header:hover {
    background-color: #f9f9f9;
}

.date-header {
    padding-left: 2.5rem;
    background-color: #fafafa;
}

.folder-icon {
    font-size: 1.2rem;
}

.folder-name {
    font-weight: 600;
    color: #2c3e50;
}

.folder-count {
    color: #666;
    font-size: 0.875rem;
}

.date-folders {
    background-color: #fafafa;
}

.date-folder {
    border-top: 1px solid #e0e0e0;
}

.files-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 1rem;
    padding: 1rem;
    padding-left: 3.5rem;
    background-color: white;
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
    .file-browser {
        padding: 1rem;
    }

    h1 {
        font-size: 1.5rem;
    }

    .search-section {
        padding: 1rem;
    }

    .filters {
        flex-direction: column;
        align-items: stretch;
    }

    .filter-group {
        width: 100%;
    }

    .filter-group input,
    .filter-group select {
        width: 100%;
    }

    .tag-select {
        width: 100%;
        min-width: auto;
    }

    .btn-clear {
        width: 100%;
        min-height: 44px;
    }

    /* Switch to list layout on mobile */
    .search-results-grid {
        grid-template-columns: 1fr;
        padding: 1rem;
    }

    .files-grid {
        grid-template-columns: 1fr;
        padding: 1rem;
        padding-left: 1rem;
    }

    .date-header {
        padding-left: 1.5rem;
    }

    .folder-header {
        padding: 0.75rem;
        min-height: 44px;
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

/* Extra small screens */
@media (max-width: 480px) {
    .file-browser {
        padding: 0.5rem;
    }

    .search-section {
        padding: 0.75rem;
    }

    .filter-group {
        font-size: 0.875rem;
    }

    .date-input,
    .tag-select {
        font-size: 0.875rem;
    }
}
</style>
