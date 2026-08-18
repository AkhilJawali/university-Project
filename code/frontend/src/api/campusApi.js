import { apiClient } from './client';

export const campusApi = {
  getAll: (params) => apiClient.get('/campuses', { params }).then((r) => r.data),

  getById: (id) => apiClient.get(`/campuses/${id}`).then((r) => r.data),

  create: (data) => apiClient.post('/campuses', data).then((r) => r.data),

  update: (id, data) => apiClient.put(`/campuses/${id}`, data).then((r) => r.data),

  delete: (id) => apiClient.delete(`/campuses/${id}`).then(() => undefined),

  getHierarchy: (id) => apiClient.get(`/campuses/${id}/hierarchy`).then((r) => r.data),
};
