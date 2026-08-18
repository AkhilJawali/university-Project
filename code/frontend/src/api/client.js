import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/login';
      return Promise.reject(error);
    }

    if (error.response?.status === 400 || error.response?.status === 409) {
      const fieldErrors = {};
      const details = error.response.data?.details;
      if (Array.isArray(details)) {
        for (const detail of details) {
          if (detail.field && detail.message) {
            fieldErrors[detail.field] = detail.message;
          }
        }
      }
      const enrichedError = Object.assign(error, { fieldErrors });
      return Promise.reject(enrichedError);
    }

    return Promise.reject(error);
  }
);
