import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { createBooking } from '../services/bookingService';
import {
  createPaymentOrder,
  loadRazorpayCheckoutScript,
  markPaymentFailed,
  verifyPaymentSignature
} from '../services/paymentService';
import { getBusDisplayName, getBusDisplayNumber } from '../utils/bus';
import { getUserId, getUserName, getUserRole, isAdminRole, isAuthenticated } from '../utils/auth';

export default function BookingConfirmation() {
  const location = useLocation();
  const navigate = useNavigate();
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState('');
  const [passengerDetails, setPassengerDetails] = useState([]);

  const bookingData = useMemo(() => location.state || {}, [location.state]);
  const seatCount = bookingData.seatNumbers?.length || 0;
  const farePerSeat = bookingData.bus?.fareInRupees || 0;
  const totalAmountInRupees = seatCount * farePerSeat;
  const adminBlocked = isAuthenticated() && isAdminRole(getUserRole());

  const selectedSeats = useMemo(
    () => [...(bookingData.seatNumbers || [])].sort((a, b) => a - b),
    [bookingData.seatNumbers]
  );

  useEffect(() => {
    setPassengerDetails(
      selectedSeats.map((seatNumber) => ({
        seatNumber,
        name: '',
        age: '',
        gender: 'MALE'
      }))
    );
  }, [selectedSeats]);

  const handlePassengerChange = (index, field, value) => {
    setPassengerDetails((prev) =>
      prev.map((passenger, passengerIndex) =>
        passengerIndex === index ? { ...passenger, [field]: value } : passenger
      )
    );
  };

  const validatePassengerDetails = () => {
    if (passengerDetails.length !== seatCount) {
      setError('Please enter passenger details for every selected seat.');
      return false;
    }

    for (let index = 0; index < passengerDetails.length; index += 1) {
      const passenger = passengerDetails[index];
      if (!passenger.name?.trim()) {
        setError(`Passenger name is required for seat ${passenger.seatNumber}`);
        return false;
      }
      if (!passenger.age || Number(passenger.age) < 1) {
        setError(`Valid passenger age is required for seat ${passenger.seatNumber}`);
        return false;
      }
      if (!passenger.gender?.trim()) {
        setError(`Passenger gender is required for seat ${passenger.seatNumber}`);
        return false;
      }
    }

    return true;
  };

  const finalizeBooking = async (userId) => {
    await createBooking({
      userId,
      busId: bookingData.busId,
      seatNumbers: selectedSeats,
      passengerDetails: passengerDetails.map(({ seatNumber, ...detail }) => ({
        ...detail,
        age: Number(detail.age)
      }))
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
    if (!validatePassengerDetails()) return;

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
      setPaying(false);
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
    <section className="section-shell">
      <div className="confirmation-hero">
        <div className="container section-space">
          <div className="section-head card redesign-results-head confirmation-header-card">
            <h2>Booking Confirmation</h2>
            <p className="muted">Review your trip details and complete payment securely.</p>
          </div>
        </div>
      </div>

      <div className="container section-space">
        <div className="card confirmation-card confirmation-enhanced redesign-checkout-card">
          <div className="payment-summary-grid confirmation-summary-grid">
            <p><strong>Bus:</strong> {getBusDisplayName(bookingData.bus)}</p>
            <p><strong>Bus No.:</strong> {getBusDisplayNumber(bookingData.bus) || bookingData.busId}</p>
            <p><strong>Selected Seats:</strong> {selectedSeats.join(', ')}</p>
            <p><strong>Fare/Seat:</strong> {farePerSeat ? `Rs. ${farePerSeat}` : 'Calculated at payment step'}</p>
            <p className="payment-total"><strong>Total:</strong> {farePerSeat ? `Rs. ${totalAmountInRupees}` : 'Calculated at payment step'}</p>
          </div>
          {bookingData.bus && (
            <p className="muted"><strong>Route:</strong> {bookingData.bus.source} to {bookingData.bus.destination}</p>
          )}

          <div className="passenger-card redesign-passenger-card">
            <div className="section-head">
              <h3>Passenger Details</h3>
              <p className="muted">Enter one passenger for each selected seat.</p>
            </div>

            <div className="passenger-list">
              {passengerDetails.map((passenger, index) => (
                <div className="passenger-row" key={passenger.seatNumber}>
                  <div className="passenger-seat">Seat {passenger.seatNumber}</div>
                  <label>
                    Full Name
                    <input
                      type="text"
                      value={passenger.name}
                      onChange={(event) => handlePassengerChange(index, 'name', event.target.value)}
                      placeholder="Passenger name"
                    />
                  </label>
                  <label>
                    Age
                    <input
                      type="number"
                      min="1"
                      value={passenger.age}
                      onChange={(event) => handlePassengerChange(index, 'age', event.target.value)}
                      placeholder="Age"
                    />
                  </label>
                  <label>
                    Gender
                    <select
                      value={passenger.gender}
                      onChange={(event) => handlePassengerChange(index, 'gender', event.target.value)}
                    >
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </label>
                </div>
              ))}
            </div>
          </div>

          {error && <p className="error">{error}</p>}

          <button type="button" className="btn btn-solid" onClick={handleConfirm} disabled={paying || adminBlocked}>
            {paying ? 'Processing Payment...' : adminBlocked ? 'Only Users Can Book' : 'Pay & Confirm Booking'}
          </button>
          <p className="muted payment-note">Your payment is processed on an encrypted gateway.</p>
        </div>
      </div>
    </section>
  );
}
