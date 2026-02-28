<template>
    <div class="settings">
        <h1>Settings</h1>

        <div class="settings-container">
            <form @submit.prevent="saveSettings" class="settings-form">
                <section class="settings-section">
                    <h2>Default Transfer Paths</h2>
                    <p class="section-description">
                        Set default paths to pre-populate the transfer form
                    </p>

                    <div class="form-group">
                        <label for="defaultSourceDir">Default Source Directory</label>
                        <input 
                            type="text" 
                            id="defaultSourceDir"
                            v-model="formData.defaultSourceDir"
                            placeholder="/path/to/source"
                            class="form-input"
                        />
                        <span class="help-text">
                            The default directory to scan for files to transfer
                        </span>
                    </div>

                    <div class="form-group">
                        <label for="defaultTargetBaseDir">Default Target Base Directory</label>
                        <input 
                            type="text" 
                            id="defaultTargetBaseDir"
                            v-model="formData.defaultTargetBaseDir"
                            placeholder="/path/to/target"
                            class="form-input"
                        />
                        <span class="help-text">
                            The default base directory where files will be organized by date
                        </span>
                    </div>
                </section>

                <section class="settings-section">
                    <h2>API Configuration</h2>
                    <p class="section-description">
                        Backend API connection settings
                    </p>

                    <div class="form-group">
                        <label>API Base URL</label>
                        <div class="readonly-field">
                            {{ settingsStore.apiBaseUrl }}
                        </div>
                        <span class="help-text">
                            The backend API endpoint (configured via environment variables)
                        </span>
                    </div>
                </section>

                <section class="settings-section" v-if="recentPaths.length > 0">
                    <h2>Recent Paths</h2>
                    <p class="section-description">
                        Recently used source and target directories
                    </p>

                    <div class="recent-paths">
                        <div v-if="settingsStore.recentSourceDirs.length > 0">
                            <h3>Recent Source Directories</h3>
                            <ul class="path-list">
                                <li v-for="(path, index) in settingsStore.recentSourceDirs" :key="index">
                                    {{ path }}
                                </li>
                            </ul>
                        </div>

                        <div v-if="settingsStore.recentTargetDirs.length > 0">
                            <h3>Recent Target Directories</h3>
                            <ul class="path-list">
                                <li v-for="(path, index) in settingsStore.recentTargetDirs" :key="index">
                                    {{ path }}
                                </li>
                            </ul>
                        </div>
                    </div>
                </section>

                <div class="form-actions">
                    <button type="submit" class="btn-save">
                        Save Settings
                    </button>
                    <button type="button" @click="clearSettings" class="btn-clear">
                        Clear All Settings
                    </button>
                </div>
            </form>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useSettingsStore } from '../stores/settings';

const settingsStore = useSettingsStore();

const formData = ref({
    defaultSourceDir: '',
    defaultTargetBaseDir: ''
});

const recentPaths = computed(() => {
    return [
        ...settingsStore.recentSourceDirs,
        ...settingsStore.recentTargetDirs
    ];
});

const loadSettings = () => {
    formData.value = {
        defaultSourceDir: settingsStore.defaultSourceDir,
        defaultTargetBaseDir: settingsStore.defaultTargetBaseDir
    };
};

const saveSettings = () => {
    // Update store state
    settingsStore.defaultSourceDir = formData.value.defaultSourceDir;
    settingsStore.defaultTargetBaseDir = formData.value.defaultTargetBaseDir;
    
    // Save to localStorage
    settingsStore.saveSettings();

    // Show success toast
    if (typeof window !== 'undefined' && (window as any).showToast) {
        (window as any).showToast('Settings saved successfully!', 'success');
    }
};

const clearSettings = () => {
    if (confirm('Are you sure you want to clear all settings? This will reset all defaults and clear recent paths.')) {
        settingsStore.clearSettings();
        loadSettings();
        
        // Show success toast
        if (typeof window !== 'undefined' && (window as any).showToast) {
            (window as any).showToast('Settings cleared successfully!', 'success');
        }
    }
};

onMounted(() => {
    loadSettings();
});
</script>

<style scoped>
.settings {
    padding: 1.5rem;
    max-width: 800px;
    margin: 0 auto;
}

h1 {
    margin-bottom: 1.5rem;
    color: #2c3e50;
}

.settings-container {
    position: relative;
}

.settings-form {
    display: flex;
    flex-direction: column;
    gap: 2rem;
}

.settings-section {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    padding: 1.5rem;
}

.settings-section h2 {
    margin: 0 0 0.5rem 0;
    color: #2c3e50;
    font-size: 1.25rem;
}

.section-description {
    margin: 0 0 1.5rem 0;
    color: #666;
    font-size: 0.875rem;
}

.form-group {
    margin-bottom: 1.5rem;
}

.form-group:last-child {
    margin-bottom: 0;
}

.form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: #2c3e50;
}

.form-input {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 1rem;
    transition: border-color 0.2s;
}

.form-input:focus {
    outline: none;
    border-color: #42b983;
}

.readonly-field {
    padding: 0.75rem;
    background: #f5f5f5;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    font-family: monospace;
    color: #666;
}

.help-text {
    display: block;
    margin-top: 0.25rem;
    font-size: 0.875rem;
    color: #666;
}

.recent-paths {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.recent-paths h3 {
    margin: 0 0 0.5rem 0;
    font-size: 1rem;
    color: #2c3e50;
}

.path-list {
    list-style: none;
    padding: 0;
    margin: 0;
}

.path-list li {
    padding: 0.5rem;
    background: #f9f9f9;
    border-radius: 4px;
    margin-bottom: 0.5rem;
    font-family: monospace;
    font-size: 0.875rem;
    color: #2c3e50;
}

.form-actions {
    display: flex;
    gap: 1rem;
    justify-content: flex-start;
}

.btn-save,
.btn-clear {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 4px;
    font-size: 1rem;
    cursor: pointer;
    transition: opacity 0.2s;
}

.btn-save {
    background: #42b983;
    color: white;
}

.btn-save:hover {
    opacity: 0.8;
}

.btn-clear {
    background: #e74c3c;
    color: white;
}

.btn-clear:hover {
    opacity: 0.8;
}

@media (max-width: 768px) {
    .settings {
        padding: 1rem;
    }

    h1 {
        font-size: 1.5rem;
    }

    .settings-section {
        padding: 1rem;
    }

    .settings-section h2 {
        font-size: 1.125rem;
    }

    .form-group label {
        font-size: 0.9375rem;
    }

    .form-input {
        font-size: 0.9375rem;
    }

    .form-actions {
        flex-direction: column;
    }

    .btn-save,
    .btn-clear {
        width: 100%;
        min-height: 44px;
    }

    .path-list li {
        font-size: 0.8125rem;
        word-break: break-all;
    }
}

/* Extra small screens */
@media (max-width: 480px) {
    .settings {
        padding: 0.5rem;
    }

    .settings-section {
        padding: 0.75rem;
    }

    .section-description {
        font-size: 0.8125rem;
    }
}
</style>
