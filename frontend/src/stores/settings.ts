/**
 * Settings Store - Pinia store for managing user preferences
 *
 * Handles user settings with localStorage persistence
 * Provides default values for transfer forms and recent path suggestions
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

/**
 * Settings state interface
 */
interface SettingsState {
    defaultSourceDir: string;
    defaultTargetBaseDir: string;
    apiBaseUrl: string;
    recentSourceDirs: string[];
    recentTargetDirs: string[];
}

/**
 * localStorage key for settings
 */
const STORAGE_KEY = 'filetrans-settings';

/**
 * Maximum number of recent paths to store
 */
const MAX_RECENT_PATHS = 10;

/**
 * Default settings
 */
const DEFAULT_SETTINGS: SettingsState = {
    defaultSourceDir: '',
    defaultTargetBaseDir: '',
    apiBaseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    recentSourceDirs: [],
    recentTargetDirs: [],
};

/**
 * Settings store
 */
export const useSettingsStore = defineStore('settings', () => {
    // ============================================================================
    // State
    // ============================================================================

    /** Default source directory */
    const defaultSourceDir = ref<string>(DEFAULT_SETTINGS.defaultSourceDir);

    /** Default target base directory */
    const defaultTargetBaseDir = ref<string>(DEFAULT_SETTINGS.defaultTargetBaseDir);

    /** API base URL */
    const apiBaseUrl = ref<string>(DEFAULT_SETTINGS.apiBaseUrl);

    /** Recent source directories */
    const recentSourceDirs = ref<string[]>([...DEFAULT_SETTINGS.recentSourceDirs]);

    /** Recent target directories */
    const recentTargetDirs = ref<string[]>([...DEFAULT_SETTINGS.recentTargetDirs]);

    // ============================================================================
    // Getters
    // ============================================================================

    /** Check if settings have been customized */
    const hasCustomSettings = computed(() => {
        return (
            defaultSourceDir.value !== DEFAULT_SETTINGS.defaultSourceDir ||
            defaultTargetBaseDir.value !== DEFAULT_SETTINGS.defaultTargetBaseDir
        );
    });

    /** Get all settings as object */
    const allSettings = computed((): SettingsState => {
        return {
            defaultSourceDir: defaultSourceDir.value,
            defaultTargetBaseDir: defaultTargetBaseDir.value,
            apiBaseUrl: apiBaseUrl.value,
            recentSourceDirs: recentSourceDirs.value,
            recentTargetDirs: recentTargetDirs.value,
        };
    });

    // ============================================================================
    // Actions
    // ============================================================================

    /**
     * Load settings from localStorage
     * Called on store initialization
     */
    function loadSettings(): void {
        try {
            const stored = localStorage.getItem(STORAGE_KEY);
            if (stored) {
                const parsed: SettingsState = JSON.parse(stored);

                // Apply stored settings
                defaultSourceDir.value =
                    parsed.defaultSourceDir || DEFAULT_SETTINGS.defaultSourceDir;
                defaultTargetBaseDir.value =
                    parsed.defaultTargetBaseDir || DEFAULT_SETTINGS.defaultTargetBaseDir;
                apiBaseUrl.value = parsed.apiBaseUrl || DEFAULT_SETTINGS.apiBaseUrl;
                recentSourceDirs.value = parsed.recentSourceDirs || [];
                recentTargetDirs.value = parsed.recentTargetDirs || [];

                console.log('[SettingsStore] Loaded settings from localStorage');
            } else {
                console.log('[SettingsStore] No stored settings found, using defaults');
            }
        } catch (err) {
            console.error('[SettingsStore] Failed to load settings from localStorage:', err);
            // Continue with default settings
        }
    }

    /**
     * Save settings to localStorage
     */
    function saveSettings(): void {
        try {
            const settings: SettingsState = {
                defaultSourceDir: defaultSourceDir.value,
                defaultTargetBaseDir: defaultTargetBaseDir.value,
                apiBaseUrl: apiBaseUrl.value,
                recentSourceDirs: recentSourceDirs.value,
                recentTargetDirs: recentTargetDirs.value,
            };

            localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
            console.log('[SettingsStore] Saved settings to localStorage');
        } catch (err) {
            console.error('[SettingsStore] Failed to save settings to localStorage:', err);
            throw new Error('Failed to save settings');
        }
    }

    /**
     * Clear all settings and reset to defaults
     */
    function clearSettings(): void {
        try {
            localStorage.removeItem(STORAGE_KEY);

            // Reset to defaults
            defaultSourceDir.value = DEFAULT_SETTINGS.defaultSourceDir;
            defaultTargetBaseDir.value = DEFAULT_SETTINGS.defaultTargetBaseDir;
            apiBaseUrl.value = DEFAULT_SETTINGS.apiBaseUrl;
            recentSourceDirs.value = [];
            recentTargetDirs.value = [];

            console.log('[SettingsStore] Cleared all settings');
        } catch (err) {
            console.error('[SettingsStore] Failed to clear settings:', err);
            throw new Error('Failed to clear settings');
        }
    }

    /**
     * Add a path to recent paths list
     * Removes duplicates and maintains max size
     */
    function addRecentPath(path: string, type: 'source' | 'target'): void {
        if (!path || path.trim() === '') {
            return;
        }

        const trimmedPath = path.trim();
        const recentList = type === 'source' ? recentSourceDirs : recentTargetDirs;

        // Remove if already exists (to move to front)
        const filtered = recentList.value.filter(p => p !== trimmedPath);

        // Add to front
        filtered.unshift(trimmedPath);

        // Limit to max size
        if (filtered.length > MAX_RECENT_PATHS) {
            filtered.splice(MAX_RECENT_PATHS);
        }

        // Update state
        if (type === 'source') {
            recentSourceDirs.value = filtered;
        } else {
            recentTargetDirs.value = filtered;
        }

        // Auto-save to localStorage
        saveSettings();

        console.log(`[SettingsStore] Added recent ${type} path: ${trimmedPath}`);
    }

    /**
     * Update default source directory
     */
    function setDefaultSourceDir(path: string): void {
        defaultSourceDir.value = path;
    }

    /**
     * Update default target base directory
     */
    function setDefaultTargetBaseDir(path: string): void {
        defaultTargetBaseDir.value = path;
    }

    /**
     * Update API base URL
     */
    function setApiBaseUrl(url: string): void {
        apiBaseUrl.value = url;
    }

    // ============================================================================
    // Initialization
    // ============================================================================

    // Load settings on store creation
    loadSettings();

    // ============================================================================
    // Return store interface
    // ============================================================================

    return {
        // State
        defaultSourceDir,
        defaultTargetBaseDir,
        apiBaseUrl,
        recentSourceDirs,
        recentTargetDirs,

        // Getters
        hasCustomSettings,
        allSettings,

        // Actions
        loadSettings,
        saveSettings,
        clearSettings,
        addRecentPath,
        setDefaultSourceDir,
        setDefaultTargetBaseDir,
        setApiBaseUrl,
    };
});
