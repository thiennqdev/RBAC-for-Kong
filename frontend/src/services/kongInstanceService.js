import api from './api';

const kongInstanceService = {
    listInstances: () => api.get('/kong-instances'),
    createInstance: (data) => api.post('/kong-instances', data),
    getInstance: (id) => api.get(`/kong-instances/${id}`),
    updateInstance: (id, data) => api.put(`/kong-instances/${id}`, data),
    deleteInstance: (id) => api.delete(`/kong-instances/${id}`),
};

export default kongInstanceService;
