import { Navigate, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './pages/Home';
import BusList from './pages/BusList';
import SeatSelection from './pages/SeatSelection';
import BookingConfirmation from './pages/BookingConfirmation';
import MyBookings from './pages/MyBookings';
import Login from './pages/Login';
import Register from './pages/Register';
import VerifyEmail from './pages/VerifyEmail';
import AdminBuses from './pages/AdminBuses';
import { getUserRole, isAdminRole, isAuthenticated } from './utils/auth';

function RequireAuth({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function RequireAdmin({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  if (!isAdminRole(getUserRole())) {
    return <Navigate to="/" replace />;
  }
  return children;
}

function RequireUserOnly({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  if (isAdminRole(getUserRole())) {
    return <Navigate to="/" replace />;
  }
  return children;
}

export default function App() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="page-wrap">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/buses" element={<BusList />} />
          <Route path="/seats/:busId" element={<SeatSelection />} />
          <Route
            path="/booking"
            element={
              <RequireAuth>
                <BookingConfirmation />
              </RequireAuth>
            }
          />
          <Route
            path="/my-bookings"
            element={
              <RequireUserOnly>
                <MyBookings />
              </RequireUserOnly>
            }
          />
          <Route
            path="/admin/buses"
            element={
              <RequireAdmin>
                <AdminBuses />
              </RequireAdmin>
            }
          />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}
