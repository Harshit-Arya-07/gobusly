import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../services/authService';
import { saveAuth } from '../utils/auth';
import { EyeIcon, EyeOffIcon } from '../components/PasswordIcons';
import BrandLogo from '../components/BrandLogo';

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    role: 'USER',
    adminSignupCode: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (form.password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      const payload = {
        name: form.name,
        email: form.email,
        password: form.password,
        role: form.role
      };

      if (form.role === 'ADMIN') {
        payload.adminSignupCode = form.adminSignupCode;
      }

      const data = await register(payload);
      saveAuth(data);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="auth-shell">
      <div className="auth-orbs">
        <span className="auth-orb auth-orb-left" />
        <span className="auth-orb auth-orb-right" />
      </div>

      <div className="container auth-layout">
        <div className="auth-copy">
          <div className="auth-brand-row">
            <BrandLogo size={32} />
            <span className="brand-name">gobusly</span>
          </div>
          <h1>Create your account</h1>
          <p className="muted large">
            Save passenger details, manage bookings, and move faster through checkout.
          </p>

          <div className="auth-points">
            <span className="hero-pill">Multi-passenger bookings</span>
            <span className="hero-pill">Ticket history</span>
            <span className="hero-pill">Offer access</span>
          </div>
        </div>

        <form className="card auth-card redesign-auth-card auth-panel" onSubmit={handleSubmit}>
          <div className="auth-header">
            <div>
              <h2>Register</h2>
              <p className="muted">Create a traveler profile in under a minute.</p>
            </div>
          </div>

          <label>
            Full Name
            <input type="text" name="name" value={form.name} onChange={handleChange} required />
          </label>

          <label>
            Email
            <input type="email" name="email" value={form.email} onChange={handleChange} required />
          </label>

          <label>
            Account Type
            <select name="role" value={form.role} onChange={handleChange}>
              <option value="USER">User</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>

          {form.role === 'ADMIN' && (
            <label>
              Admin Signup Code
              <input
                type="password"
                name="adminSignupCode"
                value={form.adminSignupCode}
                onChange={handleChange}
                required
              />
            </label>
          )}

          <label>
            Password
            <div className="password-field">
              <input
                type={showPassword ? 'text' : 'password'}
                name="password"
                value={form.password}
                onChange={handleChange}
                required
                minLength={6}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOffIcon /> : <EyeIcon />}
              </button>
            </div>
          </label>

          {error && <p className="error">{error}</p>}

          <button className="btn btn-solid" type="submit" disabled={submitting}>
            {submitting ? 'Creating...' : 'Register'}
          </button>

          <p className="muted auth-switch">
            Already registered? <Link to="/login">Login</Link>
          </p>
        </form>
      </div>
    </section>
  );
}
