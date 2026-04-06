import { useNavigate } from 'react-router-dom';
import { getUserRole, isAdminRole, isAuthenticated } from '../utils/auth';
import { formatDateTime } from '../utils/time';

export default function BusCard({ bus }) {
  const navigate = useNavigate();
  const adminBlocked = isAuthenticated() && isAdminRole(getUserRole());

  return (
    <article className="card bus-card">
      <div className="bus-card-content">
        <h3>{bus.busNumber}</h3>
        <p className="muted bus-route">{bus.source} to {bus.destination}</p>
        <p><strong>Departure:</strong> {formatDateTime(bus.time)}</p>
        <div className="bus-meta-row">
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
    </article>
  );
}
