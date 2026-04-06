import { useEffect, useMemo, useState } from 'react';
import {
  createBus,
  deleteBus,
  getAllBuses,
  updateBus
} from '../services/busService';
import { cancelBooking, getAllBookings } from '../services/bookingService';
import { getAllPaymentHistory } from '../services/paymentService';
import { formatDateTime } from '../utils/time';

const initialForm = {
  busNumber: '',
  source: '',
  destination: '',
  time: '',
  totalSeats: 40,
  fareInRupees: 500
};

export default function AdminBuses() {
  const [buses, setBuses] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [payments, setPayments] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editingId, setEditingId] = useState(null);
  const [loadingBuses, setLoadingBuses] = useState(true);
  const [loadingBookings, setLoadingBookings] = useState(true);
  const [loadingPayments, setLoadingPayments] = useState(true);
  const [saving, setSaving] = useState(false);
  const [bookingActionId, setBookingActionId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const headingText = useMemo(
    () => (editingId ? 'Update Bus' : 'Create Bus'),
    [editingId]
  );

  async function loadBuses() {
    try {
      setLoadingBuses(true);
      setError('');
      const data = await getAllBuses();
      setBuses(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load buses');
    } finally {
      setLoadingBuses(false);
    }
  }

  async function loadBookings() {
    try {
      setLoadingBookings(true);
      setError('');
      const data = await getAllBookings();
      setBookings(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load bookings');
    } finally {
      setLoadingBookings(false);
    }
  }

  async function loadPayments() {
    try {
      setLoadingPayments(true);
      setError('');
      const data = await getAllPaymentHistory();
      setPayments(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load payment history');
    } finally {
      setLoadingPayments(false);
    }
  }

  async function refreshAdminData() {
    await Promise.all([loadBuses(), loadBookings(), loadPayments()]);
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
    setEditingId(null);
  };

  const handleEdit = (bus) => {
    setEditingId(bus.id);
    setMessage('');
    setError('');
    setForm({
      busNumber: bus.busNumber,
      source: bus.source,
      destination: bus.destination,
      time: bus.time?.slice(0, 16) || '',
      totalSeats: bus.totalSeats,
      fareInRupees: bus.fareInRupees ?? ''
    });
  };

  const handleDelete = async (busId) => {
    const confirmed = window.confirm('Are you sure you want to delete this bus?');
    if (!confirmed) return;

    try {
      setError('');
      setMessage('');
      await deleteBus(busId);
      setMessage('Bus deleted successfully.');
      await refreshAdminData();
      if (editingId === busId) {
        resetForm();
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Delete failed');
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const payload = {
      ...form,
      fareInRupees: Number(form.fareInRupees),
      totalSeats: Number(form.totalSeats),
      time: new Date(form.time).toISOString().slice(0, 19)
    };

    try {
      setSaving(true);
      setError('');
      setMessage('');

      if (editingId) {
        await updateBus(editingId, payload);
        setMessage('Bus updated successfully.');
      } else {
        await createBus(payload);
        setMessage('Bus created successfully.');
      }

      resetForm();
      await refreshAdminData();
    } catch (err) {
      setError(err.response?.data?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const handleCancelBooking = async (bookingId) => {
    const confirmed = window.confirm('Cancel this booking and free up its seats?');
    if (!confirmed) return;

    try {
      setBookingActionId(bookingId);
      setError('');
      setMessage('');
      await cancelBooking(bookingId);
      setMessage('Booking cancelled successfully.');
      await refreshAdminData();
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to cancel booking');
    } finally {
      setBookingActionId(null);
    }
  };

  const totalBuses = buses.length;
  const totalSeats = buses.reduce((sum, bus) => sum + (bus.totalSeats || 0), 0);
  const confirmedBookings = bookings.filter((booking) => booking.status === 'CONFIRMED').length;
  const cancelledBookings = bookings.filter((booking) => booking.status === 'CANCELLED').length;
  const successfulPayments = payments.filter((payment) => payment.status === 'SUCCESS').length;
  const failedPayments = payments.filter((payment) => payment.status === 'FAILED').length;

  return (
    <section className="container section-space">
      <div className="section-head">
        <h2>Admin Dashboard</h2>
        <p className="muted">Manage buses, monitor bookings, and perform operational actions.</p>
      </div>

      <div className="admin-stats-grid">
        <article className="card admin-stat-card">
          <p className="admin-stat-label">Total Buses</p>
          <h3>{totalBuses}</h3>
        </article>
        <article className="card admin-stat-card">
          <p className="admin-stat-label">Total Seats</p>
          <h3>{totalSeats}</h3>
        </article>
        <article className="card admin-stat-card">
          <p className="admin-stat-label">Confirmed Bookings</p>
          <h3>{confirmedBookings}</h3>
        </article>
        <article className="card admin-stat-card">
          <p className="admin-stat-label">Cancelled Bookings</p>
          <h3>{cancelledBookings}</h3>
        </article>
        <article className="card admin-stat-card">
          <p className="admin-stat-label">Successful Payments</p>
          <h3>{successfulPayments}</h3>
        </article>
        <article className="card admin-stat-card">
          <p className="admin-stat-label">Failed Payments</p>
          <h3>{failedPayments}</h3>
        </article>
      </div>

      <div className="admin-layout">
        <form className="card admin-form" onSubmit={handleSubmit}>
          <h3>{headingText}</h3>

          <label>
            Bus Number
            <input
              name="busNumber"
              value={form.busNumber}
              onChange={handleChange}
              required
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
              {saving ? 'Saving...' : editingId ? 'Update Bus' : 'Create Bus'}
            </button>
            {editingId && (
              <button
                className="btn btn-outline"
                type="button"
                onClick={resetForm}
                disabled={saving}
              >
                Cancel Edit
              </button>
            )}
          </div>
        </form>

        <div className="card admin-list">
          <h3>All Buses</h3>
          {loadingBuses && <p>Loading buses...</p>}
          {!loadingBuses && buses.length === 0 && <p className="muted">No buses found.</p>}

          <div className="admin-bus-items">
            {buses.map((bus) => (
              <article key={bus.id} className="admin-bus-item">
                <div>
                  <h4>{bus.busNumber}</h4>
                  <p className="muted">{bus.source} to {bus.destination}</p>
                  <p><strong>Time:</strong> {formatDateTime(bus.time)}</p>
                  <p><strong>Seats:</strong> {bus.totalSeats}</p>
                  <p><strong>Fare:</strong> Rs. {bus.fareInRupees ?? '-'}/seat</p>
                </div>
                <div className="admin-item-actions">
                  <button className="btn btn-outline" type="button" onClick={() => handleEdit(bus)}>
                    Edit
                  </button>
                  <button className="btn btn-danger" type="button" onClick={() => handleDelete(bus.id)}>
                    Delete
                  </button>
                </div>
              </article>
            ))}
          </div>
        </div>
      </div>

      <div className="card admin-bookings-panel">
        <h3>All Bookings</h3>
        <p className="muted">View latest bookings and cancel confirmed ones when needed.</p>

        {loadingBookings && <p>Loading bookings...</p>}
        {!loadingBookings && bookings.length === 0 && <p className="muted">No bookings found.</p>}

        <div className="admin-booking-items">
          {bookings.map((booking) => (
            <article key={booking.id} className="admin-booking-item">
              <div>
                <h4>Booking {booking.id?.slice(-6)?.toUpperCase() || booking.id}</h4>
                <p><strong>Bus ID:</strong> {booking.busId}</p>
                <p><strong>User ID:</strong> {booking.userId}</p>
                <p><strong>Seats:</strong> {booking.seatNumbers?.join(', ') || '-'}</p>
                <p><strong>Booked At:</strong> {formatDateTime(booking.bookingTime)}</p>
                <p>
                  <strong>Status:</strong>{' '}
                  <span className={`status ${booking.status === 'CONFIRMED' ? 'confirmed' : 'cancelled'}`}>
                    {booking.status}
                  </span>
                </p>
              </div>

              {booking.status === 'CONFIRMED' && (
                <button
                  className="btn btn-danger"
                  type="button"
                  onClick={() => handleCancelBooking(booking.id)}
                  disabled={bookingActionId === booking.id}
                >
                  {bookingActionId === booking.id ? 'Cancelling...' : 'Cancel Booking'}
                </button>
              )}
            </article>
          ))}
        </div>
      </div>

      <div className="card admin-bookings-panel">
        <h3>Payment Ledger</h3>
        <p className="muted">Track every payment attempt with status and gateway IDs.</p>

        {loadingPayments && <p>Loading payment history...</p>}
        {!loadingPayments && payments.length === 0 && <p className="muted">No payment history found.</p>}

        <div className="admin-booking-items">
          {payments.map((payment) => (
            <article key={payment.id} className="admin-booking-item">
              <div>
                <h4>Payment {payment.id?.slice(-6)?.toUpperCase() || payment.id}</h4>
                <p><strong>User ID:</strong> {payment.userId}</p>
                <p><strong>Bus ID:</strong> {payment.busId}</p>
                <p><strong>Seats:</strong> {payment.seatNumbers?.join(', ') || '-'}</p>
                <p><strong>Amount:</strong> Rs. {payment.amountInRupees ?? '-'}</p>
                <p><strong>Order ID:</strong> {payment.orderId || '-'}</p>
                <p><strong>Payment ID:</strong> {payment.paymentId || '-'}</p>
                <p><strong>Receipt:</strong> {payment.receipt || '-'}</p>
                <p><strong>Reason:</strong> {payment.failureReason || '-'}</p>
                <p><strong>Time:</strong> {formatDateTime(payment.createdAt)}</p>
                <p>
                  <strong>Status:</strong>{' '}
                  <span className={`status ${payment.status === 'SUCCESS' ? 'confirmed' : payment.status === 'FAILED' ? 'cancelled' : ''}`}>
                    {payment.status}
                  </span>
                </p>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
