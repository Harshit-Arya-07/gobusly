import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import SeatGrid from '../components/SeatGrid';
import { getSeatsByBusId } from '../services/seatService';
import { getUserRole, isAdminRole, isAuthenticated } from '../utils/auth';

export default function SeatSelection() {
  const { busId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [seats, setSeats] = useState([]);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const adminBlocked = isAuthenticated() && isAdminRole(getUserRole());

  useEffect(() => {
    async function fetchSeats() {
      try {
        setLoading(true);
        setError('');
        const data = await getSeatsByBusId(busId);
        setSeats(data);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to fetch seats');
      } finally {
        setLoading(false);
      }
    }

    fetchSeats();
  }, [busId]);

  const handleToggleSeat = (seatNumber, isBooked) => {
    if (isBooked) return;
    setSelectedSeats((prev) =>
      prev.includes(seatNumber)
        ? prev.filter((num) => num !== seatNumber)
        : [...prev, seatNumber]
    );
  };

  const availableCount = useMemo(() => seats.filter((s) => !s.isBooked).length, [seats]);

  const proceedToBooking = () => {
    if (!selectedSeats.length) return;

    navigate('/booking', {
      state: {
        busId,
        seatNumbers: selectedSeats.sort((a, b) => a - b),
        bus: location.state?.bus || null
      }
    });
  };

  return (
    <section className="container section-space">
      <div className="section-head">
        <h2>Seat Selection</h2>
        <p className="muted">Select your preferred seats and proceed to payment.</p>
      </div>

      <div className="legend">
        <span><i className="dot available"></i> Available</span>
        <span><i className="dot booked"></i> Booked</span>
        <span><i className="dot selected"></i> Selected</span>
      </div>

      {loading && <p>Loading seats...</p>}
      {error && <p className="error">{error}</p>}

      {!loading && !error && (
        <div className="seat-selection-layout">
          <div className="card seat-grid-card">
            <p className="muted"><strong>Available seats:</strong> {availableCount}</p>
            {location.state?.bus?.fareInRupees && (
              <p className="muted"><strong>Fare:</strong> Rs. {location.state.bus.fareInRupees} per seat</p>
            )}
            <SeatGrid seats={seats} selectedSeats={selectedSeats} onToggle={handleToggleSeat} />
          </div>

          <aside className="booking-panel card booking-panel-sticky">
            <h3>Trip Summary</h3>
            <p><strong>Bus ID:</strong> {busId}</p>
            <p><strong>Selected Seats:</strong> {selectedSeats.length ? selectedSeats.sort((a, b) => a - b).join(', ') : 'None'}</p>
            {adminBlocked && (
              <p className="error">Admin users cannot book buses. Please login as a user account.</p>
            )}
            <button
              type="button"
              className="btn btn-solid"
              onClick={proceedToBooking}
              disabled={!selectedSeats.length || adminBlocked}
            >
              {adminBlocked ? 'Only Users Can Book' : 'Continue to Booking'}
            </button>
          </aside>
        </div>
      )}
    </section>
  );
}
