import api from './api';

export async function login(payload) {
  const { data } = await api.post('/auth/login', payload, {
    headers: {
      'Content-Type': 'application/json'
    }
  });
  return data;
}

export async function register(payload) {
  const { data } = await api.post('/auth/register', payload);
  return data;
}

export async function verifyEmailOtp(payload) {
  const { data } = await api.post('/auth/verify-email', payload);
  return data;
}

export async function resendEmailOtp(payload) {
  const { data } = await api.post('/auth/resend-otp', payload);
  return data;
}
