import { apiClient } from './client';

export const batchApi = {
  getAll: (params) => apiClient.get('/batches', { params }).then((r) => r.data),

  getById: (id) => apiClient.get(`/batches/${id}`).then((r) => r.data),

  create: (data) => apiClient.post('/batches', data).then((r) => r.data),

  update: (id, data) => apiClient.put(`/batches/${id}`, data).then((r) => r.data),

  delete: (id) => apiClient.delete(`/batches/${id}`).then(() => undefined),
};
