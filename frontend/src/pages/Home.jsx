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
    <section className="hero redesign-hero">
      <div className="hero-bg-orb hero-bg-orb-left"></div>
      <div className="hero-bg-orb hero-bg-orb-right"></div>

      <div className="hero-grid container redesign-hero-grid">
        <div className="hero-copy redesign-hero-copy">
          <p className="eyebrow">Journey Beyond Destinations</p>
          <h1>
            Book Fast.
            <span className="hero-highlight"> Ride Calm.</span>
          </h1>
          <p className="muted large">
            Discover buses across routes, pick your exact seats, and confirm booking in a frictionless flow.
          </p>
          <div className="hero-points redesign-pill-row">
            <span className="hero-pill">10,000+ Routes</span>
            <span className="hero-pill">Live Seat Map</span>
            <span className="hero-pill">Secure Checkout</span>
          </div>
        </div>

        <form className="card search-form hero-search-card redesign-search-card" onSubmit={handleSubmit}>
          <h2>Find Your Bus</h2>

          <div className="search-row-grid">
            <label>
              From
              <input
                name="source"
                value={form.source}
                onChange={handleChange}
                placeholder="e.g. Delhi"
                required
              />
            </label>

            <label>
              To
              <input
                name="destination"
                value={form.destination}
                onChange={handleChange}
                placeholder="e.g. Jaipur"
                required
              />
            </label>
          </div>

          <label>
            Travel Date
            <input name="date" type="date" value={form.date} onChange={handleChange} />
          </label>

          <button className="btn btn-solid" type="submit">Search Buses</button>
        </form>
      </div>
    </section>
  );
}
