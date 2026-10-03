import api from './api';

const resourceService = {
    list: (projectCode) => api.get(`/projects/${projectCode}/resources`),
    get: (projectCode, id) => api.get(`/projects/${projectCode}/resources/${id}`),
    create: (projectCode, data) => api.post(`/projects/${projectCode}/resources`, data),
    delete: (projectCode, id) => api.delete(`/projects/${projectCode}/resources/${id}`),
    addAction: (projectCode, resourceId, data) => api.post(`/projects/${projectCode}/resources/${resourceId}/actions`, data),
    removeAction: (projectCode, actionId) => api.delete(`/projects/${projectCode}/resources/actions/${actionId}`),
};

export default resourceService;

