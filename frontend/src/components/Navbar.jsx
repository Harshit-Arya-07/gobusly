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
  const navigate = useNavigate();
  const loggedIn = isAuthenticated();
  const userName = getUserName();
  const userRole = getUserRole();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <header className="topbar">
      <div className="container topbar-inner">
        <Link to="/" className="brand" aria-label="gobusly home">
          <BrandLogo size={34} />
          <span>gobusly</span>
        </Link>
        <nav className="topnav">
          <NavLink to="/" className="nav-item">Home</NavLink>
          <NavLink to="/buses" className="nav-item">Buses</NavLink>
          {loggedIn && !isAdminRole(userRole) && (
            <NavLink to="/my-bookings" className="nav-item">My Bookings</NavLink>
          )}
          {loggedIn && isAdminRole(userRole) && (
            <NavLink to="/admin/buses" className="nav-item">Admin Panel</NavLink>
          )}
        </nav>
        <div className="top-actions">
          {loggedIn ? (
            <>
              <span className="user-chip">{userName || 'Traveler'}</span>
              <button type="button" className="btn btn-outline" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-outline">Login</Link>
              <Link to="/register" className="btn btn-solid">Register</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
