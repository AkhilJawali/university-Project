import { apiClient } from './client';

export const sectionApi = {
  getAll: (params) => apiClient.get('/sections', { params }).then((r) => r.data),

  getById: (id) => apiClient.get(`/sections/${id}`).then((r) => r.data),

  create: (data) => apiClient.post('/sections', data).then((r) => r.data),

  update: (id, data) => apiClient.put(`/sections/${id}`, data).then((r) => r.data),

  delete: (id) => apiClient.delete(`/sections/${id}`).then(() => undefined),
};
