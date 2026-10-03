import api from './api';

const fileService = {
    async uploadFile(file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('category', 'project-icons');

        const response = await api.post('/files/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data; // Assumes response.data contains the path or object with { path }
    },
    getFileUrl(path) {
        if (!path) return '';
        if (path.startsWith('http')) return path;
        return `http://localhost:9000/devops-platform/${path}`;
    }
};

export default fileService;
