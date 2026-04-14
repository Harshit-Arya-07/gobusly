import BrandLogo from './BrandLogo';

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-grid">
        <div className="footer-brand-block">
          <div className="footer-brand">
            <span className="brand-mark footer-brand-mark"><BrandLogo size={28} /></span>
            <span className="brand-name">Gobusly</span>
          </div>
          <p className="muted footer-copy">
            Your trusted partner for comfortable, affordable, and secure bus travel across India.
          </p>
          <div className="footer-socials">
            <a href="#" aria-label="Facebook">f</a>
            <a href="#" aria-label="Twitter">t</a>
            <a href="#" aria-label="Instagram">i</a>
            <a href="#" aria-label="YouTube">y</a>
          </div>
        </div>

        <div>
          <h4>Quick Links</h4>
          <ul className="footer-links">
            <li><a href="/">Home</a></li>
            <li><a href="/buses">Buses</a></li>
            <li><a href="/my-bookings">My Bookings</a></li>
            <li><a href="/offers">Offers</a></li>
          </ul>
        </div>

        <div>
          <h4>Support</h4>
          <ul className="footer-links">
            <li><a href="#">Help Center</a></li>
            <li><a href="#">Cancellation Policy</a></li>
            <li><a href="#">Terms & Conditions</a></li>
            <li><a href="#">Privacy Policy</a></li>
          </ul>
        </div>

        <div>
          <h4>Contact Us</h4>
          <ul className="footer-links footer-contact">
            <li>Email: harshitarya0257@gmail.com</li>
            <li>Phone: +91 9509079278</li>
            <li>Available 24/7</li>
          </ul>
        </div>
      </div>

      <div className="container footer-bottom">
        <p>© 2026 Gobusly. All rights reserved.</p>
      </div>
    </footer>
  );
}