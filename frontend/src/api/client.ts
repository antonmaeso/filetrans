/**
 * Axios HTTP client with interceptors for API communication
 *
 * Configures base URL, timeout, authentication headers, and error handling
 */

import axios, { AxiosError } from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import type { ApiError } from './types';

/**
 * Base URL for API requests
 * Uses environment variable or defaults to empty string (relies on Vite proxy in dev)
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

/**
 * Request timeout in milliseconds (30 seconds)
 */
const REQUEST_TIMEOUT = 30000;

/**
 * Axios instance configured for the file transfer API
 */
const apiClient: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    timeout: REQUEST_TIMEOUT,
    headers: {
        'Content-Type': 'application/json',
    },
});

/**
 * Request interceptor
 * Adds authentication headers and other request modifications
 */
apiClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // Add authentication token if available
        // For now, this is a placeholder for future authentication implementation
        const token = localStorage.getItem('auth_token');
        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // Log request in development
        if (import.meta.env.DEV) {
            console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, {
                params: config.params,
                data: config.data,
            });
        }

        return config;
    },
    (error: AxiosError) => {
        console.error('[API Request Error]', error);
        return Promise.reject(error);
    }
);

/**
 * Response interceptor
 * Handles common error responses and logging
 */
apiClient.interceptors.response.use(
    (response: AxiosResponse) => {
        // Log response in development
        if (import.meta.env.DEV) {
            console.log(
                `[API Response] ${response.config.method?.toUpperCase()} ${response.config.url}`,
                {
                    status: response.status,
                    data: response.data,
                }
            );
        }

        return response;
    },
    (error: AxiosError<ApiError>) => {
        // Handle different error scenarios
        if (error.response) {
            // Server responded with error status (4xx, 5xx)
            const status = error.response.status;
            const apiError = error.response.data;

            console.error(`[API Error ${status}]`, {
                url: error.config?.url,
                method: error.config?.method,
                status,
                error: apiError,
            });

            // Handle specific status codes
            if (status === 401) {
                // Unauthorized - clear auth token and redirect to login
                localStorage.removeItem('auth_token');
                // Future: trigger login redirect or event
                console.warn('Authentication required - token cleared');
            } else if (status === 403) {
                // Forbidden
                console.error('Access forbidden - insufficient permissions');
            } else if (status === 404) {
                // Not found
                console.warn('Resource not found:', error.config?.url);
            } else if (status >= 500) {
                // Server error
                console.error('Server error - please try again later');
            }

            // Enhance error with structured data
            const enhancedError = new Error(
                apiError?.message || error.message || 'An error occurred'
            ) as Error & { status?: number; apiError?: ApiError };
            enhancedError.status = status;
            enhancedError.apiError = apiError;

            return Promise.reject(enhancedError);
        } else if (error.request) {
            // Request made but no response received (network error)
            console.error('[Network Error]', {
                url: error.config?.url,
                method: error.config?.method,
                message: 'No response from server - check network connection',
            });

            const networkError = new Error(
                'Network error - unable to reach server. Please check your connection.'
            ) as Error & { isNetworkError: boolean };
            networkError.isNetworkError = true;

            return Promise.reject(networkError);
        } else {
            // Error setting up the request
            console.error('[Request Setup Error]', error.message);
            return Promise.reject(error);
        }
    }
);

/**
 * Export configured Axios instance
 */
export default apiClient;

/**
 * Helper function to check if error is a network error
 */
export function isNetworkError(error: unknown): boolean {
    return (error as Error & { isNetworkError?: boolean })?.isNetworkError === true;
}

/**
 * Helper function to extract API error message
 */
export function getErrorMessage(error: unknown): string {
    if (error instanceof Error) {
        const apiError = (error as Error & { apiError?: ApiError }).apiError;
        return apiError?.message || error.message;
    }
    return 'An unexpected error occurred';
}

/**
 * Helper function to get error status code
 */
export function getErrorStatus(error: unknown): number | undefined {
    return (error as Error & { status?: number })?.status;
}
