import api from './api';

export const fileExplorerService = {
  listFiles: async (projectCode, prefix = '') => {
    const response = await api.get(`/api/v1/projects/${projectCode}/storage/explorer`, {
      params: { prefix }
    });
    return response.data;
  },

  listAllFolders: async (projectCode) => {
    const response = await api.get(`/api/v1/projects/${projectCode}/storage/explorer/folders`);
    return response.data;
  },

  getUploadUrl: async (projectCode, filePath) => {
    const response = await api.get(`/api/v1/projects/${projectCode}/storage/explorer/upload-url`, {
      params: { filePath }
    });
    return response.data;
  },

  getDownloadUrl: async (projectCode, filePath, expiryHours = null, inline = false) => {
    const params = { filePath, inline };
    if (expiryHours) params.expiryHours = expiryHours;
    const response = await api.get(`/api/v1/projects/${projectCode}/storage/explorer/download-url`, {
      params
    });
    return response.data;
  },

  deleteFile: async (projectCode, filePath) => {
    const response = await api.delete(`/api/v1/projects/${projectCode}/storage/explorer`, {
      params: { filePath }
    });
    return response.data;
  },

  bulkDeleteFiles: async (projectCode, filePaths) => {
    const response = await api.post(`/api/v1/projects/${projectCode}/storage/explorer/bulk-delete`, filePaths);
    return response.data;
  },

  createFolder: async (projectCode, folderPath) => {
    const response = await api.post(`/api/v1/projects/${projectCode}/storage/explorer/folder`, null, {
      params: { folderPath }
    });
    return response.data;
  },

  setFolderPermission: async (projectCode, folderPath, isPublic) => {
    const response = await api.post(`/api/v1/projects/${projectCode}/storage/explorer/folder/permission`, {
      folderPath,
      isPublic
    });
    return response.data;
  },

  getStorageUsage: async (projectCode) => {
    const response = await api.get(`/api/v1/projects/${projectCode}/storage/explorer/usage`);
    return response.data;
  },

  uploadFileDirectly: async (uploadUrl, file) => {
    // Note: This uploads directly to MinIO, so we don't use the configured 'api' axios instance
    // which has Authorization headers for the backend. We use standard fetch.
    const response = await fetch(uploadUrl, {
      method: 'PUT',
      body: file,
      headers: {
        'Content-Type': file.type || 'application/octet-stream'
      }
    });
    if (!response.ok) {
      throw new Error(`Upload failed with status ${response.status}`);
    }
    return true;
  }
};
