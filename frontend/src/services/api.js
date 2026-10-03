import axios from 'axios';
import keycloak from '../config/keycloak';

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
        'x-api-version': '1.0',
    },
});

// Attach JWT token to every request
api.interceptors.request.use(
    async (config) => {
        if (keycloak.token) {
            try {
                await keycloak.updateToken(5);
            } catch {
                // Token refresh failed, will use existing token
            }
            config.headers.Authorization = `Bearer ${keycloak.token}`;
            console.debug('[API] Token attached, roles:', keycloak.tokenParsed?.realm_access?.roles);
        } else {
            console.warn('[API] No token available!');
        }

        // Add kongInstanceId to Kong API requests
        if (config.url && config.url.includes('/kong/')) {
            const savedKong = localStorage.getItem('selectedKongInstance');
            if (savedKong) {
                try {
                    const instance = JSON.parse(savedKong);
                    if (instance && instance.id) {
                        config.params = { ...config.params, kongInstanceId: instance.id };
                    }
                } catch (e) {
                    console.error('Failed to parse selectedKongInstance from localStorage', e);
                }
            }
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// Handle error responses globally
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const message =
            error.response?.data?.message || error.message || 'An unexpected error occurred';
        const status = error.response?.status;
        const errorCode = error.response?.data?.error;

        if (status === 403) {
            console.error('[API] 403 Forbidden!', {
                url: error.config?.url,
                method: error.config?.method,
                responseData: error.response?.data,
                hasToken: !!error.config?.headers?.Authorization,
            });
        }

        // Construct enriched error
        const enrichedError = new Error(message);
        enrichedError.status = status;
        enrichedError.errorCode = errorCode;
        enrichedError.originalError = error;

        return Promise.reject(enrichedError);
    }
);

export default api;
