<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useTransferStore } from '../stores/transfers';
import { useSettingsStore } from '../stores/settings';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import ErrorMessage from '../components/ErrorMessage.vue';
import { validatePath, validateExtensions, type ValidationResult } from '../utils/validators';
import type { CreateTransferRequest } from '../api/types';

const router = useRouter();
const transferStore = useTransferStore();
const settingsStore = useSettingsStore();

// ============================================================================
// Form State
// ============================================================================

const sourceDir = ref<string>('');
const targetBaseDir = ref<string>('');
const filePath = ref<string>('');
const extensionsInput = ref<string>('');

// ============================================================================
// Validation State
// ============================================================================

const sourceDirError = ref<string>('');
const targetBaseDirError = ref<string>('');
const filePathError = ref<string>('');
const extensionsError = ref<string>('');
const formError = ref<string>('');

// ============================================================================
// UI State
// ============================================================================

const isSubmitting = ref<boolean>(false);
const showSourceDirSuggestions = ref<boolean>(false);
const showTargetDirSuggestions = ref<boolean>(false);

// ============================================================================
// Computed Properties
// ============================================================================

const sourceDirSuggestions = computed(() => {
    if (!sourceDir.value || sourceDir.value.trim() === '') {
        return settingsStore.recentSourceDirs;
    }

    const query = sourceDir.value.toLowerCase();
    return settingsStore.recentSourceDirs.filter(path => path.toLowerCase().includes(query));
});

const targetDirSuggestions = computed(() => {
    if (!targetBaseDir.value || targetBaseDir.value.trim() === '') {
        return settingsStore.recentTargetDirs;
    }

    const query = targetBaseDir.value.toLowerCase();
    return settingsStore.recentTargetDirs.filter(path => path.toLowerCase().includes(query));
});

const hasValidationErrors = computed(() => {
    return !!(
        sourceDirError.value ||
        targetBaseDirError.value ||
        filePathError.value ||
        extensionsError.value
    );
});

// ============================================================================
// Lifecycle
// ============================================================================

onMounted(() => {
    // Pre-populate form with saved defaults
    if (settingsStore.defaultSourceDir) {
        sourceDir.value = settingsStore.defaultSourceDir;
    }
    if (settingsStore.defaultTargetBaseDir) {
        targetBaseDir.value = settingsStore.defaultTargetBaseDir;
    }
});

// ============================================================================
// Validation Functions
// ============================================================================

function validateSourceDir(): boolean {
    sourceDirError.value = '';

    // Either sourceDir or filePath must be provided
    if (!sourceDir.value && !filePath.value) {
        sourceDirError.value = 'Either Source Directory or File Path must be provided';
        return false;
    }

    // If sourceDir is provided, validate it
    if (sourceDir.value) {
        const result: ValidationResult = validatePath(sourceDir.value, 'Source Directory');
        if (!result.valid) {
            sourceDirError.value = result.error || 'Invalid source directory';
            return false;
        }
    }

    return true;
}

function validateTargetBaseDir(): boolean {
    targetBaseDirError.value = '';

    const result: ValidationResult = validatePath(targetBaseDir.value, 'Target Base Directory');
    if (!result.valid) {
        targetBaseDirError.value = result.error || 'Invalid target directory';
        return false;
    }

    return true;
}

function validateFilePath(): boolean {
    filePathError.value = '';

    // Either sourceDir or filePath must be provided
    if (!sourceDir.value && !filePath.value) {
        filePathError.value = 'Either Source Directory or File Path must be provided';
        return false;
    }

    // If filePath is provided, validate it
    if (filePath.value) {
        const result: ValidationResult = validatePath(filePath.value, 'File Path');
        if (!result.valid) {
            filePathError.value = result.error || 'Invalid file path';
            return false;
        }
    }

    return true;
}

function validateExtensionsInput(): boolean {
    extensionsError.value = '';

    if (!extensionsInput.value) {
        return true; // Extensions are optional
    }

    const result: ValidationResult = validateExtensions(extensionsInput.value);
    if (!result.valid) {
        extensionsError.value = result.error || 'Invalid extensions';
        return false;
    }

    return true;
}

function validateForm(): boolean {
    const isSourceDirValid = validateSourceDir();
    const isTargetBaseDirValid = validateTargetBaseDir();
    const isFilePathValid = validateFilePath();
    const isExtensionsValid = validateExtensionsInput();

    // Check mutual exclusivity
    if (sourceDir.value && filePath.value) {
        formError.value = 'Cannot specify both Source Directory and File Path. Choose one.';
        return false;
    }

    // Check at least one is provided
    if (!sourceDir.value && !filePath.value) {
        formError.value = 'Either Source Directory or File Path must be provided';
        return false;
    }

    formError.value = '';
    return isSourceDirValid && isTargetBaseDirValid && isFilePathValid && isExtensionsValid;
}

// ============================================================================
// Form Handlers
// ============================================================================

function selectSourceDirSuggestion(path: string): void {
    sourceDir.value = path;
    showSourceDirSuggestions.value = false;
    sourceDirError.value = '';
}

function selectTargetDirSuggestion(path: string): void {
    targetBaseDir.value = path;
    showTargetDirSuggestions.value = false;
    targetBaseDirError.value = '';
}

function handleSourceDirFocus(): void {
    if (settingsStore.recentSourceDirs.length > 0) {
        showSourceDirSuggestions.value = true;
    }
}

function handleTargetDirFocus(): void {
    if (settingsStore.recentTargetDirs.length > 0) {
        showTargetDirSuggestions.value = true;
    }
}

function handleSourceDirBlur(): void {
    // Delay to allow click on suggestion
    setTimeout(() => {
        showSourceDirSuggestions.value = false;
    }, 200);
}

function handleTargetDirBlur(): void {
    // Delay to allow click on suggestion
    setTimeout(() => {
        showTargetDirSuggestions.value = false;
    }, 200);
}

async function handleSubmit(): Promise<void> {
    // Clear previous errors
    formError.value = '';

    // Validate form
    if (!validateForm()) {
        return;
    }

    isSubmitting.value = true;

    try {
        // Parse extensions
        let extensions: string[] | undefined;
        if (extensionsInput.value && extensionsInput.value.trim() !== '') {
            extensions = extensionsInput.value
                .split(',')
                .map(ext => ext.trim())
                .filter(ext => ext !== '');
        }

        // Build request
        const request: CreateTransferRequest = {
            targetBaseDir: targetBaseDir.value.trim(),
            extensions,
        };

        // Add either sourceDir or filePath
        if (sourceDir.value) {
            request.sourceDir = sourceDir.value.trim();
        } else if (filePath.value) {
            request.filePath = filePath.value.trim();
        }

        // Create transfer
        const response = await transferStore.createTransfer(request);

        // Add to recent paths
        if (request.sourceDir) {
            settingsStore.addRecentPath(request.sourceDir, 'source');
        }
        settingsStore.addRecentPath(request.targetBaseDir, 'target');

        // Show success toast
        if (typeof window !== 'undefined' && (window as any).showToast) {
            (window as any).showToast(`Transfer created successfully (ID: ${response.executionId})`, 'success');
        }

        // Navigate to transfer status page
        await router.push(`/transfers/${response.executionId}`);
    } catch (err) {
        if (err instanceof Error) {
            formError.value = err.message;
        } else {
            formError.value = 'Failed to create transfer. Please try again.';
        }
        console.error('[NewTransfer] Submit error:', err);
    } finally {
        isSubmitting.value = false;
    }
}

function handleReset(): void {
    sourceDir.value = settingsStore.defaultSourceDir;
    targetBaseDir.value = settingsStore.defaultTargetBaseDir;
    filePath.value = '';
    extensionsInput.value = '';

    sourceDirError.value = '';
    targetBaseDirError.value = '';
    filePathError.value = '';
    extensionsError.value = '';
    formError.value = '';
}
</script>

<template>
    <div class="new-transfer">
        <div class="new-transfer-header">
            <h1>Create New Transfer</h1>
            <p class="subtitle">Transfer files from source to target directory organized by date</p>
        </div>

        <form class="transfer-form" @submit.prevent="handleSubmit">
            <!-- Form Error -->
            <ErrorMessage v-if="formError" :message="formError" @retry="handleSubmit" />

            <!-- Source Directory -->
            <div class="form-group">
                <label for="sourceDir" class="form-label">
                    Source Directory
                    <span class="optional">(for bulk transfer)</span>
                </label>
                <div class="autocomplete-wrapper">
                    <input
                        id="sourceDir"
                        v-model="sourceDir"
                        type="text"
                        class="form-input"
                        :class="{ 'input-error': sourceDirError }"
                        placeholder="/path/to/source/directory"
                        :disabled="!!filePath"
                        @focus="handleSourceDirFocus"
                        @blur="handleSourceDirBlur"
                        @input="sourceDirError = ''"
                    />
                    <div
                        v-if="showSourceDirSuggestions && sourceDirSuggestions.length > 0"
                        class="suggestions"
                    >
                        <div
                            v-for="suggestion in sourceDirSuggestions"
                            :key="suggestion"
                            class="suggestion-item"
                            @click="selectSourceDirSuggestion(suggestion)"
                        >
                            {{ suggestion }}
                        </div>
                    </div>
                </div>
                <span v-if="sourceDirError" class="error-text">{{ sourceDirError }}</span>
                <span v-else class="help-text">Directory containing files to transfer</span>
            </div>

            <!-- OR Divider -->
            <div class="or-divider">
                <span>OR</span>
            </div>

            <!-- File Path -->
            <div class="form-group">
                <label for="filePath" class="form-label">
                    File Path
                    <span class="optional">(for single file transfer)</span>
                </label>
                <input
                    id="filePath"
                    v-model="filePath"
                    type="text"
                    class="form-input"
                    :class="{ 'input-error': filePathError }"
                    placeholder="/path/to/single/file.jpg"
                    :disabled="!!sourceDir"
                    @input="filePathError = ''"
                />
                <span v-if="filePathError" class="error-text">{{ filePathError }}</span>
                <span v-else class="help-text">Path to a single file to transfer</span>
            </div>

            <!-- Target Base Directory -->
            <div class="form-group">
                <label for="targetBaseDir" class="form-label">
                    Target Base Directory
                    <span class="required">*</span>
                </label>
                <div class="autocomplete-wrapper">
                    <input
                        id="targetBaseDir"
                        v-model="targetBaseDir"
                        type="text"
                        class="form-input"
                        :class="{ 'input-error': targetBaseDirError }"
                        placeholder="/path/to/target/directory"
                        required
                        @focus="handleTargetDirFocus"
                        @blur="handleTargetDirBlur"
                        @input="targetBaseDirError = ''"
                    />
                    <div
                        v-if="showTargetDirSuggestions && targetDirSuggestions.length > 0"
                        class="suggestions"
                    >
                        <div
                            v-for="suggestion in targetDirSuggestions"
                            :key="suggestion"
                            class="suggestion-item"
                            @click="selectTargetDirSuggestion(suggestion)"
                        >
                            {{ suggestion }}
                        </div>
                    </div>
                </div>
                <span v-if="targetBaseDirError" class="error-text">{{ targetBaseDirError }}</span>
                <span v-else class="help-text"
                    >Files will be organized as YYYY/YYYY-MM-DD under this directory</span
                >
            </div>

            <!-- Extensions -->
            <div class="form-group">
                <label for="extensions" class="form-label">
                    File Extensions
                    <span class="optional">(optional)</span>
                </label>
                <input
                    id="extensions"
                    v-model="extensionsInput"
                    type="text"
                    class="form-input"
                    :class="{ 'input-error': extensionsError }"
                    placeholder="jpg, jpeg, png, mp4"
                    @input="extensionsError = ''"
                />
                <span v-if="extensionsError" class="error-text">{{ extensionsError }}</span>
                <span v-else class="help-text"
                    >Comma-separated list of extensions to filter (leave empty for all files)</span
                >
            </div>

            <!-- Form Actions -->
            <div class="form-actions">
                <button
                    type="button"
                    class="btn btn-secondary"
                    :disabled="isSubmitting"
                    @click="handleReset"
                >
                    Reset
                </button>
                <button
                    type="submit"
                    class="btn btn-primary"
                    :disabled="isSubmitting || hasValidationErrors"
                >
                    <LoadingSpinner v-if="isSubmitting" size="small" />
                    <span v-else>Create Transfer</span>
                </button>
            </div>
        </form>
    </div>
</template>

<style scoped>
.new-transfer {
    max-width: 800px;
    margin: 0 auto;
    padding: 24px;
}

.new-transfer-header {
    margin-bottom: 32px;
}

.new-transfer-header h1 {
    margin: 0 0 8px 0;
    font-size: 28px;
    font-weight: 600;
    color: #111827;
}

.subtitle {
    margin: 0;
    font-size: 14px;
    color: #6b7280;
}

.transfer-form {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 32px;
}

.form-group {
    margin-bottom: 24px;
}

.form-label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 500;
    color: #374151;
}

.required {
    color: #ef4444;
}

.optional {
    font-weight: 400;
    color: #9ca3af;
    font-size: 13px;
}

.autocomplete-wrapper {
    position: relative;
}

.form-input {
    width: 100%;
    padding: 10px 12px;
    font-size: 14px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    transition:
        border-color 0.2s,
        box-shadow 0.2s;
    box-sizing: border-box;
}

.form-input:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input:disabled {
    background-color: #f9fafb;
    color: #9ca3af;
    cursor: not-allowed;
}

.form-input.input-error {
    border-color: #ef4444;
}

.form-input.input-error:focus {
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.suggestions {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    margin-top: 4px;
    background: white;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    max-height: 200px;
    overflow-y: auto;
    z-index: 10;
}

.suggestion-item {
    padding: 10px 12px;
    font-size: 14px;
    color: #374151;
    cursor: pointer;
    transition: background-color 0.15s;
}

.suggestion-item:hover {
    background-color: #f3f4f6;
}

.suggestion-item:first-child {
    border-top-left-radius: 6px;
    border-top-right-radius: 6px;
}

.suggestion-item:last-child {
    border-bottom-left-radius: 6px;
    border-bottom-right-radius: 6px;
}

.error-text {
    display: block;
    margin-top: 6px;
    font-size: 13px;
    color: #ef4444;
}

.help-text {
    display: block;
    margin-top: 6px;
    font-size: 13px;
    color: #6b7280;
}

.or-divider {
    position: relative;
    text-align: center;
    margin: 24px 0;
}

.or-divider::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    height: 1px;
    background-color: #e5e7eb;
}

.or-divider span {
    position: relative;
    display: inline-block;
    padding: 0 16px;
    background: white;
    color: #9ca3af;
    font-size: 13px;
    font-weight: 500;
}

.form-actions {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
    margin-top: 32px;
    padding-top: 24px;
    border-top: 1px solid #e5e7eb;
}

.btn {
    padding: 10px 20px;
    font-size: 14px;
    font-weight: 500;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition:
        background-color 0.2s,
        transform 0.1s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-width: 120px;
}

.btn:active {
    transform: translateY(1px);
}

.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
}

.btn-primary {
    background-color: #3b82f6;
    color: white;
}

.btn-primary:hover:not(:disabled) {
    background-color: #2563eb;
}

.btn-secondary {
    background-color: #f3f4f6;
    color: #374151;
}

.btn-secondary:hover:not(:disabled) {
    background-color: #e5e7eb;
}

/* Responsive Design */
@media (max-width: 768px) {
    .new-transfer {
        padding: 16px;
    }

    .transfer-form {
        padding: 20px;
    }

    .new-transfer-header h1 {
        font-size: 24px;
    }

    .form-actions {
        flex-direction: column-reverse;
    }

    .btn {
        width: 100%;
    }
}
</style>
