import api from './api';

const userService = {
    list: (params = {}) => api.get('/users', { params }),
    get: (id) => api.get(`/users/${id}`),
    create: (data) => api.post('/users', data),
    update: (id, data) => api.put(`/users/${id}`, data),
    lock: (id) => api.put(`/users/${id}/lock`),
    unlock: (id) => api.put(`/users/${id}/unlock`),
    search: (keyword = '') => api.get('/users/search', { params: { keyword } }),
    syncToKeycloak: (id) => api.post(`/users/${id}/sync`),
    syncAll: () => api.post('/users/sync-all'),
};

export default userService;
