import api from './api';

export async function createPaymentOrder(payload) {
  const { data } = await api.post('/payments/order', payload);
  return data;
}

export async function verifyPaymentSignature(payload) {
  const { data } = await api.post('/payments/verify', payload);
  return data;
}

export async function markPaymentFailed(payload) {
  const { data } = await api.post('/payments/fail', payload);
  return data;
}

export async function getPaymentHistoryByUser(userId) {
  const { data } = await api.get(`/payments/user/${userId}`);
  return data;
}

export async function getAllPaymentHistory() {
  const { data } = await api.get('/payments/admin');
  return data;
}

export function loadRazorpayCheckoutScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}
