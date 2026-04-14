import { useNavigate } from 'react-router-dom';
import { getBusDisplayName, getBusDisplayNumber } from '../utils/bus';
import { getUserRole, isAdminRole, isAuthenticated } from '../utils/auth';
import { formatDateTime } from '../utils/time';

export default function BusCard({ bus }) {
  const navigate = useNavigate();
  const adminBlocked = isAuthenticated() && isAdminRole(getUserRole());
  const seatsLeft = bus.availableSeats ?? bus.totalSeats ?? 0;

  return (
    <article className="card bus-card redesign-bus-card">
      <div className="bus-card-content redesign-bus-main">
        <div>
          <h3>{getBusDisplayName(bus)}</h3>
          <p className="muted bus-route">
            {getBusDisplayNumber(bus) ? `Bus No. ${getBusDisplayNumber(bus)} · ` : ''}
            {bus.source} to {bus.destination}
          </p>
          <p><strong>Departure:</strong> {formatDateTime(bus.time)}</p>
        </div>

        <div className="bus-metrics">
          <p><strong>Seats Left:</strong> {seatsLeft}</p>
          <p><strong>Total Seats:</strong> {bus.totalSeats}</p>
          <p className="bus-fare"><strong>Fare:</strong> Rs. {bus.fareInRupees ?? '-'}/seat</p>
        </div>
      </div>

      <button
        type="button"
        className="btn btn-solid"
        onClick={() => {
          if (adminBlocked) return;
          navigate(`/seats/${bus.id}`, { state: { bus } });
        }}
        disabled={adminBlocked}
      >
        {adminBlocked ? 'Only Users Can Book' : 'View Seats'}
      </button>

      {seatsLeft > 0 && seatsLeft <= 5 && (
        <div className="seat-alert">Only {seatsLeft} seats remaining - book soon</div>
      )}
    </article>
  );
}
