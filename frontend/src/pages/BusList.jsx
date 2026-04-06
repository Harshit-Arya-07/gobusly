import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import BusCard from '../components/BusCard';
import { getAllBuses, searchBuses } from '../services/busService';

export default function BusList() {
  const [searchParams] = useSearchParams();
  const [buses, setBuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterText, setFilterText] = useState('');
  const [sortBy, setSortBy] = useState('time-asc');

  const source = searchParams.get('source') || '';
  const destination = searchParams.get('destination') || '';
  const date = searchParams.get('date') || '';

  useEffect(() => {
    async function fetchBuses() {
      try {
        setLoading(true);
        setError('');

        let data;
        if (source || destination || date) {
          data = await searchBuses(source, destination, date);
        } else {
          data = await getAllBuses();
        }
        setBuses(data);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load buses');
      } finally {
        setLoading(false);
      }
    }

    fetchBuses();
  }, [source, destination, date]);

  const displayBuses = useMemo(() => {
    const normalized = filterText.trim().toLowerCase();

    const filtered = buses.filter((bus) => {
      if (!normalized) return true;

      return [bus.busNumber, bus.source, bus.destination]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(normalized));
    });

    return [...filtered].sort((a, b) => {
      if (sortBy === 'time-desc') {
        return new Date(b.time) - new Date(a.time);
      }
      if (sortBy === 'seats-desc') {
        return (b.totalSeats || 0) - (a.totalSeats || 0);
      }
      if (sortBy === 'seats-asc') {
        return (a.totalSeats || 0) - (b.totalSeats || 0);
      }
      return new Date(a.time) - new Date(b.time);
    });
  }, [buses, filterText, sortBy]);

  const searchSummary = [source || 'Any Source', destination || 'Any Destination']
    .join(' to ');

  return (
    <section className="container section-space">
      <div className="section-head">
        <h2>Available Buses</h2>
        <p className="muted">{searchSummary}{date ? ` | ${date}` : ''}</p>
      </div>

      <div className="results-metrics">
        <article className="card metric-card">
          <p className="muted">Results</p>
          <h3>{displayBuses.length}</h3>
        </article>
        <article className="card metric-card">
          <p className="muted">Route</p>
          <h3>{source || 'Any'} to {destination || 'Any'}</h3>
        </article>
        <article className="card metric-card">
          <p className="muted">Travel Date</p>
          <h3>{date || 'Flexible'}</h3>
        </article>
      </div>

      <div className="card bus-filters">
        <label>
          Quick Filter
          <input
            value={filterText}
            onChange={(event) => setFilterText(event.target.value)}
            placeholder="Search by bus number, source, destination"
          />
        </label>
        <label>
          Sort By
          <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
            <option value="time-asc">Departure Time: Earliest First</option>
            <option value="time-desc">Departure Time: Latest First</option>
            <option value="seats-desc">Seats: High to Low</option>
            <option value="seats-asc">Seats: Low to High</option>
          </select>
        </label>
      </div>

      {loading && <p>Loading buses...</p>}
      {error && <p className="error">{error}</p>}

      {!loading && !error && displayBuses.length === 0 && (
        <div className="card empty-state">No buses found for this route.</div>
      )}

      <div className="grid-cards">
        {displayBuses.map((bus) => (
          <BusCard key={bus.id} bus={bus} />
        ))}
      </div>
    </section>
  );
}
