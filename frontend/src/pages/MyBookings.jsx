import { useEffect, useState } from 'react';
import { cancelBooking, getBookingsByUser } from '../services/bookingService';
import { getPaymentHistoryByUser } from '../services/paymentService';
import { getUserId } from '../utils/auth';
import { formatDateTime } from '../utils/time';

export default function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [paymentHistory, setPaymentHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadBookings = async () => {
    const userId = getUserId();
    if (!userId) {
      setLoading(false);
      setError('User not logged in');
      return;
    }

    try {
      setLoading(true);
      setError('');
      const [bookingsData, paymentData] = await Promise.all([
        getBookingsByUser(userId),
        getPaymentHistoryByUser(userId)
      ]);
      setBookings(bookingsData);
      setPaymentHistory(paymentData);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to fetch bookings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const handleCancel = async (bookingId) => {
    try {
      await cancelBooking(bookingId);
      await loadBookings();
    } catch (err) {
      setError(err.response?.data?.message || 'Cancellation failed');
    }
  };

  return (
    <section className="container section-space">
      <h2>My Bookings</h2>
      {loading && <p>Loading bookings...</p>}
      {error && <p className="error">{error}</p>}

      {!loading && !error && bookings.length === 0 && (
        <div className="card empty-state">No bookings found.</div>
      )}

      <div className="grid-cards">
        {bookings.map((booking) => (
          <article className="card booking-card" key={booking.id}>
            <h3>Booking #{booking.id}</h3>
            <p><strong>Bus:</strong> {booking.busId}</p>
            <p><strong>Seats:</strong> {booking.seatNumbers.join(', ')}</p>
            <p><strong>Status:</strong> <span className={`status ${booking.status.toLowerCase()}`}>{booking.status}</span></p>
            <p><strong>Time:</strong> {formatDateTime(booking.bookingTime)}</p>

            {booking.status === 'CONFIRMED' && (
              <button type="button" className="btn btn-danger" onClick={() => handleCancel(booking.id)}>
                Cancel Booking
              </button>
            )}
          </article>
        ))}
      </div>

      <section className="section-space">
        <h2>Payment History</h2>
        {!loading && !error && paymentHistory.length === 0 && (
          <div className="card empty-state">No payment history found.</div>
        )}

        <div className="grid-cards">
          {paymentHistory.map((payment) => (
            <article className="card booking-card" key={payment.id}>
              <h3>Payment #{payment.id?.slice(-8)?.toUpperCase()}</h3>
              <p><strong>Bus:</strong> {payment.busId}</p>
              <p><strong>Seats:</strong> {payment.seatNumbers?.join(', ') || '-'}</p>
              <p><strong>Amount:</strong> Rs. {payment.amountInRupees}</p>
              <p>
                <strong>Status:</strong>{' '}
                <span className={`status ${payment.status?.toLowerCase()}`}>
                  {payment.status}
                </span>
              </p>
              <p><strong>Order ID:</strong> {payment.orderId}</p>
              {payment.paymentId && <p><strong>Payment ID:</strong> {payment.paymentId}</p>}
              {payment.failureReason && <p className="error"><strong>Failure:</strong> {payment.failureReason}</p>}
              <p><strong>Time:</strong> {formatDateTime(payment.createdAt)}</p>
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}
