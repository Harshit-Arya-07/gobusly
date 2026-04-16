import axios from 'axios';
import { clearAuth, getToken } from '../utils/auth';

const API = import.meta.env.VITE_API_BASE_URL;
const apiBaseUrl = (API || '/api').replace(/\/$/, '');

const api = axios.create({
  baseURL: apiBaseUrl
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
