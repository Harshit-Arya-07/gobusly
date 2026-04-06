import { useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { createBooking } from '../services/bookingService';
import {
  createPaymentOrder,
  loadRazorpayCheckoutScript,
  markPaymentFailed,
  verifyPaymentSignature
} from '../services/paymentService';
import { getUserId, getUserName, getUserRole, isAdminRole, isAuthenticated } from '../utils/auth';

export default function BookingConfirmation() {
  const location = useLocation();
  const navigate = useNavigate();
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState('');

  const bookingData = useMemo(() => location.state || {}, [location.state]);
  const seatCount = bookingData.seatNumbers?.length || 0;
  const farePerSeat = bookingData.bus?.fareInRupees || 0;
  const totalAmountInRupees = seatCount * farePerSeat;
  const adminBlocked = isAuthenticated() && isAdminRole(getUserRole());

  const finalizeBooking = async (userId) => {
    await createBooking({
      userId,
      busId: bookingData.busId,
      seatNumbers: bookingData.seatNumbers
    });
    navigate('/my-bookings');
  };

  const handleConfirm = async () => {
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }

    if (adminBlocked) {
      setError('Admin users are not allowed to book buses. Please use a user account.');
      return;
    }

    const userId = getUserId();
    if (!userId || !bookingData.busId || !bookingData.seatNumbers?.length) return;

    try {
      setPaying(true);
      setError('');

      const scriptLoaded = await loadRazorpayCheckoutScript();
      if (!scriptLoaded) {
        throw new Error('Unable to load Razorpay checkout SDK');
      }

      const paymentOrder = await createPaymentOrder({
        userId,
        busId: bookingData.busId,
        seatNumbers: bookingData.seatNumbers
      });

      const options = {
        key: paymentOrder.keyId,
        amount: paymentOrder.amount,
        currency: paymentOrder.currency,
        order_id: paymentOrder.orderId,
        name: 'gobusly',
        description: `Bus Booking Payment (${seatCount} seat${seatCount > 1 ? 's' : ''})`,
        prefill: {
          name: getUserName() || 'Traveler'
        },
        theme: {
          color: '#0b8f77'
        },
        handler: async function handler(response) {
          try {
            await verifyPaymentSignature({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature
            });

            await finalizeBooking(userId);
          } catch (verificationError) {
            setError(verificationError.response?.data?.message || 'Payment verification failed');
          } finally {
            setPaying(false);
          }
        },
        modal: {
          ondismiss: () => {
            setPaying(false);
          }
        }
      };

      const razorpayInstance = new window.Razorpay(options);
      razorpayInstance.on('payment.failed', async (failure) => {
        try {
          await markPaymentFailed({
            razorpayOrderId: failure.error?.metadata?.order_id || paymentOrder.orderId,
            razorpayPaymentId: failure.error?.metadata?.payment_id || null,
            reason: failure.error?.description || 'Payment failed'
          });
        } catch (markFailedError) {
          // Intentionally ignored to avoid blocking user from retrying checkout.
        }
        setError(failure.error?.description || 'Payment failed. Please try again.');
        setPaying(false);
      });
      razorpayInstance.open();
    } catch (err) {
      setError(err.response?.data?.message || 'Booking failed. Try again.');
    }
  };

  if (!bookingData.busId || !bookingData.seatNumbers?.length) {
    return (
      <section className="container section-space">
        <div className="card empty-state">
          <p>No booking data found.</p>
          <Link className="btn btn-solid" to="/buses">Go to Buses</Link>
        </div>
      </section>
    );
  }

  return (
    <section className="container section-space">
      <div className="section-head">
        <h2>Booking Confirmation</h2>
        <p className="muted">Review your trip details and complete payment securely.</p>
      </div>

      <div className="card confirmation-card confirmation-enhanced">
        <div className="payment-summary-grid">
          <p><strong>Bus ID:</strong> {bookingData.busId}</p>
          <p><strong>Selected Seats:</strong> {bookingData.seatNumbers.join(', ')}</p>
          <p><strong>Fare/Seat:</strong> {farePerSeat ? `Rs. ${farePerSeat}` : 'Calculated at payment step'}</p>
          <p className="payment-total"><strong>Total:</strong> {farePerSeat ? `Rs. ${totalAmountInRupees}` : 'Calculated at payment step'}</p>
        </div>
        {bookingData.bus && (
          <p className="muted"><strong>Route:</strong> {bookingData.bus.source} to {bookingData.bus.destination}</p>
        )}

        {error && <p className="error">{error}</p>}

        <button type="button" className="btn btn-solid" onClick={handleConfirm} disabled={paying || adminBlocked}>
          {paying ? 'Processing Payment...' : adminBlocked ? 'Only Users Can Book' : 'Pay & Confirm Booking'}
        </button>
        <p className="muted payment-note">Your payment is processed on an encrypted gateway.</p>
      </div>
    </section>
  );
}
