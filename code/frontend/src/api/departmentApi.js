import { apiClient } from './client';

export const departmentApi = {
  getAll: (params) => apiClient.get('/departments', { params }).then((r) => r.data),

  getById: (id) => apiClient.get(`/departments/${id}`).then((r) => r.data),

  create: (data) => apiClient.post('/departments', data).then((r) => r.data),

  update: (id, data) => apiClient.put(`/departments/${id}`, data).then((r) => r.data),

  delete: (id) => apiClient.delete(`/departments/${id}`).then(() => undefined),
};
