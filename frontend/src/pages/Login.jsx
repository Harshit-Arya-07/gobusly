import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../services/authService';
import { saveAuth } from '../utils/auth';
import { EyeIcon, EyeOffIcon } from '../components/PasswordIcons';
import BrandLogo from '../components/BrandLogo';

export default function Login() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [loginAs, setLoginAs] = useState('USER');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      setSubmitting(true);
      setError('');
      const data = await login(form);
      const actualRole = String(data?.role || 'USER').toUpperCase();

      if (actualRole !== loginAs) {
        setError(`This account is ${actualRole}. Please select ${actualRole} and login again.`);
        return;
      }

      saveAuth(data, { rememberMe });
      navigate(actualRole === 'ADMIN' ? '/admin/buses' : '/');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
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
          <h1>Welcome Back</h1>
          <p className="muted large">
            Sign in to continue your booking flow, manage trips, and access your tickets.
          </p>

          <div className="auth-points">
            <span className="hero-pill">Fast checkout</span>
            <span className="hero-pill">Saved passenger details</span>
            <span className="hero-pill">Secure payments</span>
          </div>
        </div>

        <form className="card auth-card redesign-auth-card auth-panel" onSubmit={handleSubmit}>
          <div className="auth-header">
            <div>
              <h2>Sign In</h2>
              <p className="muted">Use the same account for your bookings and payments.</p>
            </div>
          </div>

          <label>
            Email
            <input type="email" name="email" value={form.email} onChange={handleChange} required />
          </label>

          <label>
            Login As
            <select value={loginAs} onChange={(event) => setLoginAs(event.target.value)}>
              <option value="USER">User</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>

          <label>
            Password
            <div className="password-field">
              <input
                type={showPassword ? 'text' : 'password'}
                name="password"
                value={form.password}
                onChange={handleChange}
                required
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

          <label className="remember-row">
            <input
              type="checkbox"
              checked={rememberMe}
              onChange={(event) => setRememberMe(event.target.checked)}
            />
            <span>Remember me for 10 minutes</span>
          </label>

          {error && <p className="error">{error}</p>}

          <button className="btn btn-solid" type="submit" disabled={submitting}>
            {submitting ? 'Signing in...' : 'Login'}
          </button>

          <p className="muted auth-switch">
            Don't have an account? <Link to="/register">Register</Link>
          </p>
        </form>
      </div>
    </section>
  );
}
