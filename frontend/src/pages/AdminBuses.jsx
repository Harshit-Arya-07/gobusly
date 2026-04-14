import { useEffect, useMemo, useState } from 'react';
import { createBus, deleteBus, getAllBuses } from '../services/busService';
import { getBusDisplayName, getBusDisplayNumber } from '../utils/bus';
import { formatDateTime } from '../utils/time';

const initialForm = {
  busNumber: '',
  busName: '',
  source: '',
  destination: '',
  time: '',
  totalSeats: 40,
  fareInRupees: 500
};

export default function AdminBuses() {
  const [buses, setBuses] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [loadingBuses, setLoadingBuses] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const headingText = useMemo(() => 'Create Bus', []);

  async function loadBuses() {
    try {
      setLoadingBuses(true);
      setError('');
      const data = await getAllBuses();
      setBuses(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load buses');
    } finally {
      setLoadingBuses(false);
    }
  }

  async function refreshAdminData() {
    await loadBuses();
  }

  useEffect(() => {
    refreshAdminData();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: ['totalSeats', 'fareInRupees'].includes(name) ? Number(value) : value
    }));
  };

  const resetForm = () => {
    setForm(initialForm);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const payload = {
      ...form,
      busName: form.busName.trim(),
      fareInRupees: Number(form.fareInRupees),
      totalSeats: Number(form.totalSeats),
      time: new Date(form.time).toISOString().slice(0, 19)
    };

    try {
      setSaving(true);
      setError('');
      setMessage('');

      await createBus(payload);
      setMessage('Bus created successfully.');

      resetForm();
      await refreshAdminData();
    } catch (err) {
      setError(err.response?.data?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (busId) => {
    const confirmed = window.confirm('Are you sure you want to delete this bus?');
    if (!confirmed) return;

    try {
      setSaving(true);
      setError('');
      setMessage('');
      await deleteBus(busId);
      setMessage('Bus deleted successfully.');
      await refreshAdminData();
    } catch (err) {
      setError(err.response?.data?.message || 'Delete failed');
    } finally {
      setSaving(false);
    }
  };

  const safeBuses = Array.isArray(buses) ? buses : [];

  const totalBuses = safeBuses.length;
  const totalSeats = safeBuses.reduce((sum, bus) => sum + (bus.totalSeats || 0), 0);

  return (
    <section className="section-shell">
      <div className="admin-hero">
        <div className="container section-space">
          <div className="section-head card redesign-results-head admin-hero-card">
            <h2>Admin Dashboard</h2>
            <p className="muted">Admin role is restricted to creating buses only.</p>
          </div>
        </div>
      </div>

      <div className="container section-space">
        <div className="admin-stats-grid">
          <article className="card admin-stat-card">
            <p className="admin-stat-label">Total Buses</p>
            <h3>{totalBuses}</h3>
          </article>
          <article className="card admin-stat-card">
            <p className="admin-stat-label">Total Seats</p>
            <h3>{totalSeats}</h3>
          </article>
        </div>

        <div className="admin-layout">
          <form className="card admin-form" onSubmit={handleSubmit}>
            <h3>{headingText}</h3>

            <label>
              Bus Name
              <input
                name="busName"
                value={form.busName}
                onChange={handleChange}
                required
                placeholder="e.g. Royal Express"
              />
            </label>

            <label>
              Bus Number
              <input
                name="busNumber"
                value={form.busNumber}
                onChange={handleChange}
                required
                placeholder="e.g. RJ-01-AB-1234"
              />
            </label>

            <label>
              Source
              <input name="source" value={form.source} onChange={handleChange} required />
            </label>

            <label>
              Destination
              <input
                name="destination"
                value={form.destination}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Departure Time
              <input
                type="datetime-local"
                name="time"
                value={form.time}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Total Seats
              <input
                type="number"
                min="1"
                name="totalSeats"
                value={form.totalSeats}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Fare (Rs per seat)
              <input
                type="number"
                min="1"
                name="fareInRupees"
                value={form.fareInRupees}
                onChange={handleChange}
                required
              />
            </label>

            {error && <p className="error">{error}</p>}
            {message && <p className="success">{message}</p>}

            <div className="admin-actions">
              <button className="btn btn-solid" type="submit" disabled={saving}>
                {saving ? 'Saving...' : 'Create Bus'}
              </button>
            </div>
          </form>

          <div className="card admin-list">
            <h3>All Buses</h3>
            {loadingBuses && <p>Loading buses...</p>}
            {!loadingBuses && safeBuses.length === 0 && <p className="muted">No buses found.</p>}

            <div className="admin-bus-items">
              {safeBuses.map((bus) => (
                <article key={bus.id} className="admin-bus-item">
                  <div>
                    <h4>{getBusDisplayName(bus)}</h4>
                    <p className="muted">{getBusDisplayNumber(bus) ? `Bus No. ${getBusDisplayNumber(bus)}` : 'Bus number unavailable'}</p>
                    <p className="muted">{bus.source} to {bus.destination}</p>
                    <p><strong>Time:</strong> {formatDateTime(bus.time)}</p>
                    <p><strong>Seats:</strong> {bus.totalSeats}</p>
                    <p><strong>Fare:</strong> Rs. {bus.fareInRupees ?? '-'}/seat</p>
                  </div>
                  <div className="admin-item-actions">
                    <button
                      className="btn btn-danger"
                      type="button"
                      onClick={() => handleDelete(bus.id)}
                      disabled={saving}
                    >
                      Delete
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
