import api from './api';

function unwrapListPayload(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (Array.isArray(payload?.content)) {
    return payload.content;
  }

  return [];
}

export async function createBooking(payload) {
  const { data } = await api.post('/bookings', payload);
  return data;
}

export async function getAllBookings() {
  const { data } = await api.get('/bookings');
  return unwrapListPayload(data);
}

export async function getBookingsByUser(userId) {
  const { data } = await api.get(`/bookings/user/${userId}`);
  return unwrapListPayload(data);
}

export async function cancelBooking(bookingId) {
  const { data } = await api.delete(`/bookings/${bookingId}`);
  return data;
}
