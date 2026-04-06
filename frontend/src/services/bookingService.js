import api from './api';

export async function createBooking(payload) {
  const { data } = await api.post('/bookings', payload);
  return data;
}

export async function getAllBookings() {
  const { data } = await api.get('/bookings');
  return data;
}

export async function getBookingsByUser(userId) {
  const { data } = await api.get(`/bookings/user/${userId}`);
  return data;
}

export async function cancelBooking(bookingId) {
  const { data } = await api.delete(`/bookings/${bookingId}`);
  return data;
}
