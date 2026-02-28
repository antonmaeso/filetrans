<template>
    <div class="main-layout">
        <!-- Connection Error Banner -->
        <div v-if="showConnectionError" class="connection-error-banner">
            <span>⚠️ Unable to connect to backend API. Please check if the server is running.</span>
            <button @click="dismissConnectionError" class="btn-dismiss">×</button>
        </div>

        <!-- Navigation Bar -->
        <nav class="navbar">
            <div class="nav-container">
                <div class="nav-brand">
                    <router-link to="/" class="brand-link">
                        📁 FileTransfer
                    </router-link>
                </div>

                <!-- Desktop Navigation -->
                <div class="nav-links desktop-nav">
                    <router-link to="/" class="nav-link" exact-active-class="active">
                        Dashboard
                    </router-link>
                    <router-link to="/transfers/new" class="nav-link" active-class="active">
                        New Transfer
                    </router-link>
                    <router-link to="/transfers" class="nav-link" active-class="active">
                        Transfer History
                    </router-link>
                    <router-link to="/files" class="nav-link" active-class="active">
                        File Browser
                    </router-link>
                    <router-link to="/settings" class="nav-link" active-class="active">
                        Settings
                    </router-link>
                </div>

                <!-- Mobile Menu Toggle -->
                <button @click="toggleMobileMenu" class="mobile-menu-toggle">
                    <span class="hamburger-icon">☰</span>
                </button>
            </div>

            <!-- Mobile Navigation -->
            <div v-if="mobileMenuOpen" class="mobile-nav">
                <router-link 
                    to="/" 
                    class="nav-link" 
                    exact-active-class="active"
                    @click="closeMobileMenu"
                >
                    Dashboard
                </router-link>
                <router-link 
                    to="/transfers/new" 
                    class="nav-link" 
                    active-class="active"
                    @click="closeMobileMenu"
                >
                    New Transfer
                </router-link>
                <router-link 
                    to="/transfers" 
                    class="nav-link" 
                    active-class="active"
                    @click="closeMobileMenu"
                >
                    Transfer History
                </router-link>
                <router-link 
                    to="/files" 
                    class="nav-link" 
                    active-class="active"
                    @click="closeMobileMenu"
                >
                    File Browser
                </router-link>
                <router-link 
                    to="/settings" 
                    class="nav-link" 
                    active-class="active"
                    @click="closeMobileMenu"
                >
                    Settings
                </router-link>
            </div>
        </nav>

        <!-- Main Content -->
        <main class="main-content">
            <router-view />
        </main>

        <!-- Toast Notification Container -->
        <div class="toast-container">
            <div 
                v-for="toast in toasts" 
                :key="toast.id"
                :class="['toast', `toast-${toast.type}`]"
            >
                <span class="toast-icon">{{ getToastIcon(toast.type) }}</span>
                <span class="toast-message">{{ toast.message }}</span>
                <button @click="removeToast(toast.id)" class="toast-close">×</button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// Mobile menu state
const mobileMenuOpen = ref(false);

// Connection error state
const showConnectionError = ref(false);

// Toast notifications
interface Toast {
    id: number;
    message: string;
    type: 'success' | 'error' | 'warning' | 'info';
}

const toasts = ref<Toast[]>([]);
let toastIdCounter = 0;

const toggleMobileMenu = () => {
    mobileMenuOpen.value = !mobileMenuOpen.value;
};

const closeMobileMenu = () => {
    mobileMenuOpen.value = false;
};

const dismissConnectionError = () => {
    showConnectionError.value = false;
};

const getToastIcon = (type: string): string => {
    switch (type) {
        case 'success': return '✓';
        case 'error': return '✕';
        case 'warning': return '⚠';
        case 'info': return 'ℹ';
        default: return '';
    }
};

const addToast = (message: string, type: Toast['type'] = 'info') => {
    const id = toastIdCounter++;
    toasts.value.push({ id, message, type });
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        removeToast(id);
    }, 5000);
};

const removeToast = (id: number) => {
    const index = toasts.value.findIndex(t => t.id === id);
    if (index !== -1) {
        toasts.value.splice(index, 1);
    }
};

// Global error handler for API errors
const handleApiError = (error: any) => {
    if (error.message?.includes('Network Error') || error.code === 'ECONNREFUSED') {
        showConnectionError.value = true;
    }
};

// Close mobile menu on route change
router.afterEach(() => {
    closeMobileMenu();
});

// Expose toast methods globally (can be accessed via provide/inject)
const showToast = (message: string, type: Toast['type'] = 'info') => {
    addToast(message, type);
};

// Make toast available globally
if (typeof window !== 'undefined') {
    (window as any).showToast = showToast;
}

onMounted(() => {
    // Listen for global API errors
    window.addEventListener('api-error', handleApiError as any);
});

onUnmounted(() => {
    window.removeEventListener('api-error', handleApiError as any);
});
</script>

<style scoped>
.main-layout {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f5f5f5;
}

.connection-error-banner {
    background: #e74c3c;
    color: white;
    padding: 1rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 0.875rem;
}

.btn-dismiss {
    background: none;
    border: none;
    color: white;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
}

.btn-dismiss:hover {
    opacity: 0.8;
}

.navbar {
    background: white;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    position: sticky;
    top: 0;
    z-index: 100;
}

.nav-container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 1rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 60px;
}

.nav-brand {
    font-size: 1.25rem;
    font-weight: 600;
}

.brand-link {
    color: #2c3e50;
    text-decoration: none;
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.brand-link:hover {
    color: #42b983;
}

.nav-links {
    display: flex;
    gap: 0.5rem;
}

.nav-link {
    padding: 0.5rem 1rem;
    color: #2c3e50;
    text-decoration: none;
    border-radius: 4px;
    transition: all 0.2s;
    font-size: 0.9375rem;
}

.nav-link:hover {
    background: #f5f5f5;
    color: #42b983;
}

.nav-link.active {
    background: #42b983;
    color: white;
}

.mobile-menu-toggle {
    display: none;
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0.5rem;
    color: #2c3e50;
}

.mobile-nav {
    display: none;
}

.main-content {
    flex: 1;
    max-width: 1400px;
    width: 100%;
    margin: 0 auto;
    padding: 0;
}

.toast-container {
    position: fixed;
    bottom: 2rem;
    right: 2rem;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    z-index: 1000;
}

.toast {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 1rem 1.25rem;
    border-radius: 4px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    min-width: 300px;
    animation: slideIn 0.3s ease-out;
}

.toast-success {
    background: #42b983;
    color: white;
}

.toast-error {
    background: #e74c3c;
    color: white;
}

.toast-warning {
    background: #f39c12;
    color: white;
}

.toast-info {
    background: #3498db;
    color: white;
}

.toast-icon {
    font-size: 1.25rem;
    font-weight: bold;
}

.toast-message {
    flex: 1;
}

.toast-close {
    background: none;
    border: none;
    color: white;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0;
    width: 1.5rem;
    height: 1.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0.8;
}

.toast-close:hover {
    opacity: 1;
}

@keyframes slideIn {
    from {
        transform: translateX(100%);
        opacity: 0;
    }
    to {
        transform: translateX(0);
        opacity: 1;
    }
}

/* Mobile Styles */
@media (max-width: 768px) {
    .desktop-nav {
        display: none;
    }

    .mobile-menu-toggle {
        display: block;
    }

    .mobile-nav {
        display: flex;
        flex-direction: column;
        padding: 1rem;
        border-top: 1px solid #e0e0e0;
    }

    .mobile-nav .nav-link {
        padding: 0.75rem 1rem;
        border-radius: 4px;
        min-height: 44px;
        display: flex;
        align-items: center;
    }

    .toast-container {
        left: 1rem;
        right: 1rem;
        bottom: 1rem;
    }

    .toast {
        min-width: auto;
    }
}
</style>
