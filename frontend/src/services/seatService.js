import api from './api';

export async function getSeatsByBusId(busId) {
  const { data } = await api.get(`/seats/${busId}`);
  return data;
}
