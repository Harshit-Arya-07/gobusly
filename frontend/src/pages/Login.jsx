import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../services/authService';
import { saveAuth } from '../utils/auth';

export default function Login() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
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
      saveAuth(data, { rememberMe });
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="container section-space auth-wrap">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h2>Welcome Back</h2>
        <label>
          Email
          <input type="email" name="email" value={form.email} onChange={handleChange} required />
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
        <p className="muted">No account? <Link to="/register">Create one</Link></p>
      </form>
    </section>
  );
}

function IconBase({ children }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      {children}
    </svg>
  );
}

function EyeIcon() {
  return (
    <IconBase>
      <path d="M2 12C3.5 7.5 7.4 5 12 5C16.6 5 20.5 7.5 22 12C20.5 16.5 16.6 19 12 19C7.4 19 3.5 16.5 2 12Z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
      <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="2" />
    </IconBase>
  );
}

function EyeOffIcon() {
  return (
    <IconBase>
      <path d="M4 4L20 20" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M10.5 10.6C10.1 11.1 9.9 11.5 9.9 12C9.9 13.1 10.8 14 12 14C12.5 14 12.9 13.8 13.4 13.4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M6.6 7.2C4.9 8.3 3.5 10 2 12C3.5 16.5 7.4 19 12 19C13.2 19 14.3 18.8 15.3 18.4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M9.9 5.9C10.6 5.7 11.3 5.5 12 5.5C16.6 5.5 20.5 8 22 12C21.4 13.7 20.4 15.2 19.1 16.4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </IconBase>
  );
}
