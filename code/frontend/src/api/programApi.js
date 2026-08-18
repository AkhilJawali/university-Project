import { apiClient } from './client';

export const programApi = {
  getAll: (params) => apiClient.get('/programs', { params }).then((r) => r.data),

  getById: (id) => apiClient.get(`/programs/${id}`).then((r) => r.data),

  create: (data) => apiClient.post('/programs', data).then((r) => r.data),

  update: (id, data) => apiClient.put(`/programs/${id}`, data).then((r) => r.data),

  delete: (id) => apiClient.delete(`/programs/${id}`).then(() => undefined),
};
