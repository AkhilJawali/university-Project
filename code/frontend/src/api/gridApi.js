import { apiClient } from './client';

export const gridApi = {
  getAll: (params) => apiClient.get('/time-slot-grids', { params }).then((r) => r.data),
  getById: (id) => apiClient.get(`/time-slot-grids/${id}`).then((r) => r.data),
  create: (data) => apiClient.post('/time-slot-grids', data).then((r) => r.data),
  update: (id, data) => apiClient.put(`/time-slot-grids/${id}`, data).then((r) => r.data),
  delete: (id) => apiClient.delete(`/time-slot-grids/${id}`).then(() => undefined),
  activate: (id) => apiClient.put(`/time-slot-grids/${id}/activate`).then((r) => r.data),

  // Slots
  getSlots: (gridId) =>
    apiClient.get(`/time-slot-grids/${gridId}/slots`).then((r) => r.data),
  createSlot: (gridId, data) =>
    apiClient.post(`/time-slot-grids/${gridId}/slots`, data).then((r) => r.data),
  bulkCreateSlots: (gridId, data) =>
    apiClient.post(`/time-slot-grids/${gridId}/slots/bulk`, data).then((r) => r.data),
  deleteSlot: (gridId, slotId) =>
    apiClient.delete(`/time-slot-grids/${gridId}/slots/${slotId}`).then(() => undefined),

  // Working Days
  getWorkingDays: (gridId) =>
    apiClient.get(`/time-slot-grids/${gridId}/working-days`).then((r) => r.data),
  updateWorkingDays: (gridId, data) =>
    apiClient.put(`/time-slot-grids/${gridId}/working-days`, data).then((r) => r.data),
};
