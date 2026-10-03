import api from './api';

const storageService = {
    getStorageConfig: (projectCode) => api.get(`/projects/${projectCode}/storage`),
    enableStorage: (projectCode, data) => api.post(`/projects/${projectCode}/storage/enable`, data),
    getPolicies: (projectCode) => api.get(`/projects/${projectCode}/storage/policies`),
    updatePolicies: (projectCode, rules) => api.put(`/projects/${projectCode}/storage/policies`, { rules }),
    rotateCredentials: (projectCode) => api.post(`/projects/${projectCode}/storage/credentials/rotate`)
};

export default storageService;
