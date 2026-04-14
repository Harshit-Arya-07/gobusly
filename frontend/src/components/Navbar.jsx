import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import BrandLogo from './BrandLogo';
import {
  clearAuth,
  getUserName,
  getUserRole,
  isAdminRole,
  isAuthenticated
} from '../utils/auth';

export default function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();
  const loggedIn = isAuthenticated();
  const userName = getUserName();
  const userRole = getUserRole();

  const handleLogout = () => {
    clearAuth();
    setMobileOpen(false);
    navigate('/login');
  };

  const closeMobileMenu = () => setMobileOpen(false);

  return (
    <header className="topbar redesign-topbar">
      <div className="container topbar-inner redesign-topbar-inner">
        <Link to="/" className="brand redesign-brand" aria-label="gobusly home" onClick={closeMobileMenu}>
          <span className="brand-mark"><BrandLogo size={28} /></span>
          <span>Gobusly</span>
        </Link>

        <nav className="topnav redesign-topnav desktop-only">
          <NavLink to="/" className="nav-item" onClick={closeMobileMenu}>Home</NavLink>
          <NavLink to="/buses" className="nav-item" onClick={closeMobileMenu}>Buses</NavLink>
          {loggedIn && !isAdminRole(userRole) && (
            <NavLink to="/my-bookings" className="nav-item" onClick={closeMobileMenu}>My Bookings</NavLink>
          )}
          {loggedIn && isAdminRole(userRole) && (
            <NavLink to="/admin/buses" className="nav-item" onClick={closeMobileMenu}>Admin Panel</NavLink>
          )}
        </nav>

        <div className="top-actions desktop-only">
          {loggedIn ? (
            <>
              <span className="user-chip">{userName || 'Traveler'}</span>
              <button type="button" className="btn btn-outline" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <Link to="/login" className="btn btn-outline">Login</Link>
          )}
        </div>

        <button
          type="button"
          className="mobile-nav-toggle"
          onClick={() => setMobileOpen((prev) => !prev)}
          aria-label={mobileOpen ? 'Close menu' : 'Open menu'}
        >
          {mobileOpen ? 'x' : 'menu'}
        </button>
      </div>

      {mobileOpen && (
        <div className="mobile-nav-panel">
          <div className="container mobile-nav-content">
            <NavLink to="/" className="nav-item" onClick={closeMobileMenu}>Home</NavLink>
            <NavLink to="/buses" className="nav-item" onClick={closeMobileMenu}>Buses</NavLink>
            {loggedIn && !isAdminRole(userRole) && (
              <NavLink to="/my-bookings" className="nav-item" onClick={closeMobileMenu}>My Bookings</NavLink>
            )}
            {loggedIn && isAdminRole(userRole) && (
              <NavLink to="/admin/buses" className="nav-item" onClick={closeMobileMenu}>Admin Panel</NavLink>
            )}

            <div className="mobile-auth-actions">
              {loggedIn ? (
                <>
                  <span className="user-chip">{userName || 'Traveler'}</span>
                  <button type="button" className="btn btn-outline" onClick={handleLogout}>Logout</button>
                </>
              ) : (
                <Link to="/login" className="btn btn-outline" onClick={closeMobileMenu}>Login</Link>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
