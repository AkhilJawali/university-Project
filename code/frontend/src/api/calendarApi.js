import { apiClient } from './client';

export const calendarApi = {
  getAll: (params) => apiClient.get('/academic-calendars', { params }).then((r) => r.data),
  getById: (id) => apiClient.get(`/academic-calendars/${id}`).then((r) => r.data),
  create: (data) => apiClient.post('/academic-calendars', data).then((r) => r.data),
  update: (id, data) => apiClient.put(`/academic-calendars/${id}`, data).then((r) => r.data),
  delete: (id) => apiClient.delete(`/academic-calendars/${id}`).then(() => undefined),

  // Holidays
  getHolidays: (calendarId) =>
    apiClient.get(`/academic-calendars/${calendarId}/holidays`).then((r) => r.data),
  createHoliday: (calendarId, data) =>
    apiClient.post(`/academic-calendars/${calendarId}/holidays`, data).then((r) => r.data),
  deleteHoliday: (calendarId, holidayId) =>
    apiClient.delete(`/academic-calendars/${calendarId}/holidays/${holidayId}`).then(() => undefined),

  // Exam Windows
  getExamWindows: (calendarId) =>
    apiClient.get(`/academic-calendars/${calendarId}/exam-windows`).then((r) => r.data),
  createExamWindow: (calendarId, data) =>
    apiClient.post(`/academic-calendars/${calendarId}/exam-windows`, data).then((r) => r.data),
  deleteExamWindow: (calendarId, windowId) =>
    apiClient.delete(`/academic-calendars/${calendarId}/exam-windows/${windowId}`).then(() => undefined),

  // Special Periods
  getSpecialPeriods: (calendarId) =>
    apiClient.get(`/academic-calendars/${calendarId}/special-periods`).then((r) => r.data),
  createSpecialPeriod: (calendarId, data) =>
    apiClient.post(`/academic-calendars/${calendarId}/special-periods`, data).then((r) => r.data),
  deleteSpecialPeriod: (calendarId, periodId) =>
    apiClient.delete(`/academic-calendars/${calendarId}/special-periods/${periodId}`).then(() => undefined),
};
