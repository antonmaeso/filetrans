import { createApp } from 'vue';
import { createPinia } from 'pinia';
import './style.css';
import './styles/variables.css';
import App from './App.vue';
import router from './router';

const app = createApp(App);

app.use(createPinia());
app.use(router);

// Global error handler for uncaught errors
app.config.errorHandler = (err, instance, info) => {
    console.error('[Global Error Handler]', err);
    console.error('Component:', instance);
    console.error('Error Info:', info);
    
    // Show toast notification if available
    if (typeof window !== 'undefined' && (window as any).showToast) {
        (window as any).showToast('An unexpected error occurred. Please try again.', 'error');
    }
};

app.mount('#app');
