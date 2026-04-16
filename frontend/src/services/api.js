import axios from 'axios';
import { clearAuth, getToken } from '../utils/auth';

const API = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
console.log('API URL:', import.meta.env.VITE_API_BASE_URL);

if (!API) {
  console.warn('VITE_API_BASE_URL is not set. API calls may fail in production.');
}

const api = axios.create({
  baseURL: API,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth();
    }
    return Promise.reject(error);
  }
);

export default api;
