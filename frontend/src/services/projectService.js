import api from './api';

const projectService = {
    list: (params = {}) => api.get('/projects', { params }),
    get: (code) => api.get(`/projects/${code}`),
    create: (data) => api.post('/projects', data),
    update: (code, data) => api.put(`/projects/${code}`, data),
    delete: (code) => api.delete(`/projects/${code}`),
    hardDelete: (code) => api.delete(`/projects/${code}/hard`),
    reactivate: (code) => api.patch(`/projects/${code}/reactivate`),
};

export default projectService;
