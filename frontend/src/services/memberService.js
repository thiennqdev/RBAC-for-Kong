import api from './api';

const memberService = {
    list: (projectCode) => api.get(`/projects/${projectCode}/members`),
    assign: (projectCode, data) => api.post(`/projects/${projectCode}/members`, data),
    batchAssign: (projectCode, data) => api.post(`/projects/${projectCode}/members/batch`, data),
    updateRole: (projectCode, userId, data) =>
        api.put(`/projects/${projectCode}/members/${userId}`, data),
    remove: (projectCode, userId) =>
        api.delete(`/projects/${projectCode}/members/${userId}`),
};

export default memberService;
