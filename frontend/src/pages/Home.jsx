import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Home() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ source: '', destination: '', date: '' });

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    const queryData = {
      source: form.source.trim(),
      destination: form.destination.trim()
    };

    if (form.date) {
      queryData.date = form.date;
    }

    const query = new URLSearchParams(queryData);
    navigate(`/buses?${query.toString()}`);
  };

  return (
    <section className="hero">
      <div className="hero-grid container">
        <div className="hero-copy">
          <p className="eyebrow">Smart Intercity Travel</p>
          <h1>Book bus seats in seconds, travel stress-free.</h1>
          <p className="muted large">
            Search routes, inspect live seat availability, and confirm bookings with a smooth, modern flow.
          </p>
          <div className="hero-points">
            <span className="hero-pill">Live seat availability</span>
            <span className="hero-pill">Secure online payment</span>
            <span className="hero-pill">Instant booking status</span>
          </div>
        </div>
        <form className="card search-form hero-search-card" onSubmit={handleSubmit}>
          <h2>Search Buses</h2>
          <label>
            Source
            <input
              name="source"
              value={form.source}
              onChange={handleChange}
              placeholder="e.g. Delhi"
              required
            />
          </label>
          <label>
            Destination
            <input
              name="destination"
              value={form.destination}
              onChange={handleChange}
              placeholder="e.g. Jaipur"
              required
            />
          </label>
          <label>
            Date
            <input name="date" type="date" value={form.date} onChange={handleChange} />
          </label>
          <button className="btn btn-solid" type="submit">Search Buses</button>
        </form>
      </div>
    </section>
  );
}
