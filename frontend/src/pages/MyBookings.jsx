import { useEffect, useMemo, useState } from 'react';
import { jsPDF } from 'jspdf';
import { cancelBooking, getBookingsByUser } from '../services/bookingService';
import { getPaymentHistoryByUser } from '../services/paymentService';
import { getAllBuses } from '../services/busService';
import { getUserId } from '../utils/auth';
import { getBusDisplayName, getBusDisplayNumber } from '../utils/bus';
import { formatDateTime } from '../utils/time';

export default function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [buses, setBuses] = useState([]);
  const [paymentHistory, setPaymentHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const busesById = useMemo(() => {
    const map = new Map();
    (Array.isArray(buses) ? buses : []).forEach((bus) => {
      if (bus?.id) {
        map.set(bus.id, bus);
      }
    });
    return map;
  }, [buses]);

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
      let busesData = [];
      try {
        busesData = await getAllBuses();
      } catch (_busError) {
        busesData = [];
      }
      setBookings(Array.isArray(bookingsData) ? bookingsData : []);
      setPaymentHistory(Array.isArray(paymentData) ? paymentData : []);
      setBuses(Array.isArray(busesData) ? busesData : []);
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

  const getBookingBus = (booking) => {
    const fallback = busesById.get(booking?.busId);
    return {
      id: booking?.busId || fallback?.id || '',
      busName: booking?.busName || fallback?.busName || '',
      busNumber: booking?.busNumber || fallback?.busNumber || '',
      source: booking?.source || fallback?.source || '-',
      destination: booking?.destination || fallback?.destination || '-',
      time: booking?.time || fallback?.time || booking?.bookingTime || null
    };
  };

  const handleDownloadTicket = (booking) => {
    const bus = getBookingBus(booking);
    const bookingId = booking?.id || 'UNKNOWN';
    const seats = Array.isArray(booking?.seatNumbers) && booking.seatNumbers.length
      ? booking.seatNumbers.join(', ')
      : '-';
    const passengerNames = Array.isArray(booking?.passengerDetails) && booking.passengerDetails.length
      ? booking.passengerDetails.map((passenger) => passenger?.name).filter(Boolean).join(', ')
      : '-';

    const doc = new jsPDF();
    doc.setFontSize(18);
    doc.text('Bus Ticket', 14, 20);

    doc.setFontSize(12);
    const rows = [
      ['Bus Name', getBusDisplayName(bus)],
      ['Bus Number', getBusDisplayNumber(bus) || '-'],
      ['Route', `${bus.source || '-'} -> ${bus.destination || '-'}`],
      ['Date & Time', formatDateTime(bus.time || booking?.bookingTime)],
      ['Seat Number', seats],
      ['Passenger Name', passengerNames],
      ['Booking ID', bookingId]
    ];

    let y = 34;
    rows.forEach(([label, value]) => {
      doc.text(`${label}: ${value}`, 14, y);
      y += 9;
    });

    const safeBookingId = String(bookingId).replace(/[^a-zA-Z0-9_-]/g, '_');
    doc.save(`Bus_Ticket_${safeBookingId}.pdf`);
  };

  const getStatus = (value) => String(value || 'PENDING').toUpperCase();

  return (
    <section className="section-shell">
      <div className="container section-space">
        <div className="section-head card redesign-results-head my-bookings-hero">
          <h2>My Bookings</h2>
          <p className="muted">Track confirmed rides, cancellations, and payments in one place.</p>
        </div>

        {loading && <p>Loading bookings...</p>}
        {error && <p className="error">{error}</p>}

        {!loading && !error && bookings.length === 0 && (
          <div className="card empty-state">No bookings found.</div>
        )}

        <div className="grid-cards bookings-grid">
          {bookings.map((booking) => (
            <article className="card booking-card booking-card-enhanced" key={booking.id}>
              {(() => {
                const status = getStatus(booking?.status);
                const isSuccess = status === 'CONFIRMED' || status === 'SUCCESS';
                const isFailed = status === 'FAILED';
                const isCancelled = status === 'CANCELLED';
                const bus = getBookingBus(booking);

                return (
                  <>
                    <div className={`booking-state-banner ${isSuccess ? 'booking-state-success' : isFailed ? 'booking-state-failed' : isCancelled ? 'booking-state-cancelled' : 'booking-state-pending'}`}>
                      {isFailed ? (
                        <>
                          <h3>Transaction Failed</h3>
                          <p>Your booking could not be completed</p>
                        </>
                      ) : isCancelled ? (
                        <>
                          <h3>Booking Cancelled</h3>
                          <p>This booking has been cancelled</p>
                        </>
                      ) : isSuccess ? (
                        <>
                          <h3>Booking Confirmed</h3>
                          <p>Your booking is successfully confirmed</p>
                        </>
                      ) : (
                        <>
                          <h3>Booking Pending</h3>
                          <p>Your booking is being processed</p>
                        </>
                      )}
                    </div>

                    <div className="booking-card-top">
                      <div className="booking-identity-stack">
                        <p className="booking-bus-name">Bus Name: {getBusDisplayName(bus)}</p>
                        {getBusDisplayNumber(bus) && (
                          <p className="booking-bus-number">Bus Number: {getBusDisplayNumber(bus)}</p>
                        )}
                        <p className="booking-bus-route">From / To: {bus.source || '-'} to {bus.destination || '-'}</p>
                        <p className="booking-bus-time">Date & Time: {formatDateTime(bus.time || booking.bookingTime)}</p>
                        <p className="booking-label">Booking</p>
                        <p className="booking-id">#{booking.id}</p>
                      </div>
                      <span className={`status ${status.toLowerCase()}`}>
                        {status}
                      </span>
                    </div>

                    <div className="booking-card-body">
                      <p><strong>Seat Number:</strong> {Array.isArray(booking.seatNumbers) && booking.seatNumbers.length ? booking.seatNumbers.join(', ') : '-'}</p>
                      {Array.isArray(booking.passengerDetails) && booking.passengerDetails.length > 0 && (
                        <div>
                          <p><strong>Passenger Name:</strong></p>
                          <ul className="passenger-summary-list">
                            {booking.passengerDetails.map((passenger, index) => (
                              <li key={`${booking.id}-${index}`}>
                                Seat {booking.seatNumbers?.[index] ?? index + 1}: {passenger?.name || '-'}
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>

                    <div className="booking-actions-row">
                      {isSuccess && (
                        <button type="button" className="btn btn-solid booking-ticket-btn" onClick={() => handleDownloadTicket(booking)}>
                          Download Ticket
                        </button>
                      )}
                      {isFailed && (
                        <button type="button" className="btn btn-outline" onClick={() => window.alert('Coming Soon')}>
                          Retry Payment
                        </button>
                      )}
                      {status === 'CONFIRMED' && (
                        <button type="button" className="btn btn-danger" onClick={() => handleCancel(booking.id)}>
                          Cancel Booking
                        </button>
                      )}
                    </div>
                  </>
                );
              })()}
            </article>
          ))}
        </div>

        <section className="section-space">
          <div className="section-head">
            <h2>Payment History</h2>
          </div>
          {!loading && !error && paymentHistory.length === 0 && (
            <div className="card empty-state">No payment history found.</div>
          )}

          <div className="grid-cards bookings-grid">
            {paymentHistory.map((payment) => (
              <article className="card booking-card booking-card-enhanced" key={payment.id}>
                <div className="booking-card-top">
                  <div>
                    <h3>Payment #{payment.id?.slice(-8)?.toUpperCase()}</h3>
                    <p className="muted">{getBusDisplayName(payment)}</p>
                    <p className="muted">{getBusDisplayNumber(payment) || payment.busId}</p>
                  </div>
                  <span className={`status ${payment.status?.toLowerCase()}`}>
                    {payment.status || 'PENDING'}
                  </span>
                </div>
                <div className="booking-card-body">
                  <p><strong>Seats:</strong> {payment.seatNumbers?.join(', ') || '-'}</p>
                  <p><strong>Amount:</strong> Rs. {payment.amountInRupees ?? '-'}</p>
                  <p><strong>Order ID:</strong> {payment.orderId}</p>
                  {payment.paymentId && <p><strong>Payment ID:</strong> {payment.paymentId}</p>}
                  {payment.failureReason && <p className="error"><strong>Failure:</strong> {payment.failureReason}</p>}
                  <p><strong>Time:</strong> {formatDateTime(payment.createdAt)}</p>
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>
    </section>
  );
}
