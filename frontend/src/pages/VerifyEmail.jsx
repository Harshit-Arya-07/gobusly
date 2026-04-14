import { useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import BrandLogo from '../components/BrandLogo';
import { resendEmailOtp, verifyEmailOtp } from '../services/authService';
import { saveAuth } from '../utils/auth';

export default function VerifyEmail() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialEmail = useMemo(() => searchParams.get('email') || '', [searchParams]);

  const [email, setEmail] = useState(initialEmail);
  const [otp, setOtp] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('Enter the OTP sent to your email to complete verification.');

  const handleVerify = async (event) => {
    event.preventDefault();

    if (!email.trim()) {
      setError('Email is required');
      return;
    }

    if (!/^\d{6}$/.test(otp.trim())) {
      setError('OTP must be a 6-digit number');
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      setInfo('');
      const data = await verifyEmailOtp({ email: email.trim(), otp: otp.trim() });
      saveAuth(data, { rememberMe: true });
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'OTP verification failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = async () => {
    if (!email.trim()) {
      setError('Email is required');
      return;
    }

    try {
      setResending(true);
      setError('');
      const response = await resendEmailOtp({ email: email.trim() });
      setInfo(response?.message || 'A new OTP has been sent.');
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to resend OTP');
    } finally {
      setResending(false);
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
          <h1>Verify Your Email</h1>
          <p className="muted large">
            Enter the OTP from your inbox. We only activate accounts after email verification.
          </p>

          <div className="auth-points">
            <span className="hero-pill">OTP valid for 10 minutes</span>
            <span className="hero-pill">Resend available</span>
            <span className="hero-pill">Auto login after verify</span>
          </div>
        </div>

        <form className="card auth-card redesign-auth-card auth-panel" onSubmit={handleVerify}>
          <div className="auth-header">
            <div>
              <h2>Email Verification</h2>
              <p className="muted">Use the 6-digit OTP sent to your email.</p>
            </div>
          </div>

          <label>
            Email
            <input
              type="email"
              name="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </label>

          <label>
            OTP
            <input
              type="text"
              name="otp"
              value={otp}
              onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
              inputMode="numeric"
              maxLength={6}
              required
            />
          </label>

          {error && <p className="error">{error}</p>}
          {!error && info && <p className="muted">{info}</p>}

          <button className="btn btn-solid" type="submit" disabled={submitting}>
            {submitting ? 'Verifying...' : 'Verify Email'}
          </button>

          <button
            type="button"
            className="btn btn-outline"
            onClick={handleResend}
            disabled={resending}
          >
            {resending ? 'Resending...' : 'Resend OTP'}
          </button>

          <p className="muted auth-switch">
            Back to <Link to="/login">Login</Link>
          </p>
        </form>
      </div>
    </section>
  );
}
