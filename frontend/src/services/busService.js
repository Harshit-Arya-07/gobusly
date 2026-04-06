import api from './api';

export async function getAllBuses() {
  const { data } = await api.get('/buses');
  return data;
}

export async function searchBuses(source, destination, date) {
  const { data } = await api.get('/buses/search', {
    params: { source, destination, date }
  });
  return data;
}

export async function createBus(payload) {
  const { data } = await api.post('/buses', payload);
  return data;
}

export async function updateBus(busId, payload) {
  const { data } = await api.put(`/buses/${busId}`, payload);
  return data;
}

export async function deleteBus(busId) {
  const { data } = await api.delete(`/buses/${busId}`);
  return data;
}
