import api from './api';

const kongService = {
    // Services
    listServices: (kongInstanceId, projectCode) => api.get(`/projects/${projectCode}/kong/services?kongInstanceId=${kongInstanceId}`),
    createService: (kongInstanceId, projectCode, data) => api.post(`/projects/${projectCode}/kong/services?kongInstanceId=${kongInstanceId}`, data),
    getService: (kongInstanceId, projectCode, id) => api.get(`/projects/${projectCode}/kong/services/${id}?kongInstanceId=${kongInstanceId}`),
    updateService: (kongInstanceId, projectCode, id, data) => api.put(`/projects/${projectCode}/kong/services/${id}?kongInstanceId=${kongInstanceId}`, data),
    deleteService: (kongInstanceId, projectCode, id) => api.delete(`/projects/${projectCode}/kong/services/${id}?kongInstanceId=${kongInstanceId}`),

    // Routes
    listRoutes: (kongInstanceId, projectCode, serviceId) => api.get(`/projects/${projectCode}/kong/services/${serviceId}/routes?kongInstanceId=${kongInstanceId}`),
    createRoute: (kongInstanceId, projectCode, serviceId, data) => api.post(`/projects/${projectCode}/kong/services/${serviceId}/routes?kongInstanceId=${kongInstanceId}`, data),
    getRoute: (kongInstanceId, projectCode, id) => api.get(`/projects/${projectCode}/kong/routes/${id}?kongInstanceId=${kongInstanceId}`),
    updateRoute: (kongInstanceId, projectCode, id, data) => api.put(`/projects/${projectCode}/kong/routes/${id}?kongInstanceId=${kongInstanceId}`, data),
    deleteRoute: (kongInstanceId, projectCode, id) => api.delete(`/projects/${projectCode}/kong/routes/${id}?kongInstanceId=${kongInstanceId}`),
    requestRouteApproval: (kongInstanceId, projectCode, id) => api.post(`/projects/${projectCode}/kong/routes/${id}/request-approval?kongInstanceId=${kongInstanceId}`),
    approveRoute: (kongInstanceId, projectCode, id) => api.post(`/projects/${projectCode}/kong/routes/${id}/approve?kongInstanceId=${kongInstanceId}`),
    rejectRoute: (kongInstanceId, projectCode, id) => api.post(`/projects/${projectCode}/kong/routes/${id}/reject?kongInstanceId=${kongInstanceId}`),

    // Upstreams
    listUpstreams: (kongInstanceId, projectCode) => api.get(`/projects/${projectCode}/kong/upstreams?kongInstanceId=${kongInstanceId}`),
    createUpstream: (kongInstanceId, projectCode, data) => api.post(`/projects/${projectCode}/kong/upstreams?kongInstanceId=${kongInstanceId}`, data),
    getUpstream: (kongInstanceId, projectCode, id) => api.get(`/projects/${projectCode}/kong/upstreams/${id}?kongInstanceId=${kongInstanceId}`),
    updateUpstream: (kongInstanceId, projectCode, id, data) => api.put(`/projects/${projectCode}/kong/upstreams/${id}?kongInstanceId=${kongInstanceId}`, data),
    deleteUpstream: (kongInstanceId, projectCode, id) => api.delete(`/projects/${projectCode}/kong/upstreams/${id}?kongInstanceId=${kongInstanceId}`),

    // Plugins
    listPlugins: (kongInstanceId, projectCode) => api.get(`/projects/${projectCode}/kong/plugins?kongInstanceId=${kongInstanceId}`),
    createPlugin: (kongInstanceId, projectCode, data) => api.post(`/projects/${projectCode}/kong/plugins?kongInstanceId=${kongInstanceId}`, data),
    getPlugin: (kongInstanceId, projectCode, id) => api.get(`/projects/${projectCode}/kong/plugins/${id}?kongInstanceId=${kongInstanceId}`),
    updatePlugin: (kongInstanceId, projectCode, id, data) => api.put(`/projects/${projectCode}/kong/plugins/${id}?kongInstanceId=${kongInstanceId}`, data),
    deletePlugin: (kongInstanceId, projectCode, id) => api.delete(`/projects/${projectCode}/kong/plugins/${id}?kongInstanceId=${kongInstanceId}`),

    // Targets (nested under Upstreams)
    listTargets: (kongInstanceId, projectCode, upstreamId) => api.get(`/projects/${projectCode}/kong/upstreams/${upstreamId}/targets?kongInstanceId=${kongInstanceId}`),
    addTarget: (kongInstanceId, projectCode, upstreamId, data) => api.post(`/projects/${projectCode}/kong/upstreams/${upstreamId}/targets?kongInstanceId=${kongInstanceId}`, data),
    deleteTarget: (kongInstanceId, projectCode, upstreamId, targetId) => api.delete(`/projects/${projectCode}/kong/upstreams/${upstreamId}/targets/${targetId}?kongInstanceId=${kongInstanceId}`),
};

export default kongService;
