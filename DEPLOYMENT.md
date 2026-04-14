# Deployment Guide - Bus Booking System

## Production Deployment Checklist

### 1. Backend (Render)

#### Build Configuration
- Language: `Docker`
- Build Command: (leave default)
- Dockerfile Path: `Dockerfile`
- Root Directory: `backend`

#### Critical Environment Variables

**Database & Caching**
```
SPRING_DATA_MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/busdb
SPRING_DATA_REDIS_HOST=redis-host.redislabs.com
SPRING_DATA_REDIS_PORT=12345
SPRING_DATA_REDIS_PASSWORD=your_redis_password
```

**JWT & Auth**
```
APP_JWT_SECRET=your-base64-encoded-secret-key
APP_JWT_EXPIRATION_MS=86400000
APP_AUTH_ADMIN_SIGNUP_CODE=your-admin-code
```

**Payment (Razorpay)**
```
APP_PAYMENT_RAZORPAY_KEY_ID=rzp_live_xxxxx (use LIVE keys in production)
APP_PAYMENT_RAZORPAY_KEY_SECRET=xxxxx
```

**Email Notifications**
```
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password (NOT regular password - use Google App Password)
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
APP_MAIL_FROM=your-email@gmail.com
```

**CORS & Frontend**
```
FRONTEND_URL=https://your-vercel-domain.vercel.app
```

**Optional**
```
APP_CACHE_CLEANUP_LEGACY_PREFIXES=true
APP_RATE_LIMIT_RPM=60
APP_RATE_LIMIT_AUTH_RPM=10
APP_SEAT_LOCK_TTL_MINUTES=5
```

### 2. Frontend (Vercel)

#### Build Configuration
- Framework: `Vite`
- Root Directory: `frontend`
- Build Command: `npm run build`
- Output Directory: `dist`
- Install Command: `npm install`

#### Required Environment Variable
```
VITE_API_BASE_URL=https://your-backend-render-domain.onrender.com/api
```

**Example:**
```
VITE_API_BASE_URL=https://gobusly-backend.onrender.com/api
```

### 3. MongoDB Setup (MongoDB Atlas)

1. Create cluster on MongoDB Atlas
2. Create database user with password
3. Get connection string: `mongodb+srv://user:pass@cluster.xxx.mongodb.net/busdb`
4. Add Render IP to IP Allowlist (use 0.0.0.0/0 for any IP, or specific IPs)

### 4. Redis Setup (Redis Labs or similar)

1. Create Redis instance
2. Get host, port, password
3. Use in `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_PASSWORD`

### 5. Gmail App Password (for email notifications)

1. Go to [myaccount.google.com](https://myaccount.google.com)
2. Security → 2-Step Verification (must be enabled)
3. Security → App Passwords
4. Select Mail, Other (gobusly-backend)
5. Copy 16-char password
6. Use as `SPRING_MAIL_PASSWORD`

### 6. Razorpay Keys

- **Development**: Use `rzp_test_...` keys
- **Production**: Use `rzp_live_...` keys (only when live account verified)
- Keys must be paired: test-key with test-secret, live-key with live-secret

---

## Testing After Deployment

### 1. Test Backend API
```bash
curl https://your-backend-render-domain.onrender.com/api/buses
# Should return: [] or bus list

curl -X POST https://your-backend-render-domain.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@example.com","password":"password123"}'
# Should register user and return JWT token
```

### 2. Test Frontend Connection
1. Open frontend URL in browser
2. Try to register/login
3. Check browser Console (F12) for errors
4. Should see API calls to backend

### 3. Check Logs
- **Render Backend Logs**: Dashboard → Logs tab
- **Vercel Frontend Logs**: Dashboard → Deployments → Logs
- Look for errors in startup or request handling

---

## Common Issues & Fixes

### Frontend shows "API not responding"
**Cause**: VITE_API_BASE_URL not set
**Fix**: Add `VITE_API_BASE_URL=https://your-backend.onrender.com/api` in Vercel Environment Variables

### CORS error in frontend
**Cause**: Backend FRONTEND_URL doesn't match deployed frontend domain
**Fix**: Set `FRONTEND_URL=https://your-frontend.vercel.app` in Render backend

### Registration fails with 500 error
**Cause**: MongoDB connection failed or duplicate email
**Fix**: 
1. Check MongoDB connection string
2. Verify IP is allowlisted in MongoDB Atlas
3. Check MongoDB credentials

### Email not sending
**Cause**: Gmail credentials wrong or 2-Step Verification not enabled
**Fix**:
1. Use App Password (not regular Gmail password)
2. Enable 2-Step Verification on Google account
3. Check `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD`

### Payments fail
**Cause**: Razorpay keys missing or wrong mode (test vs live)
**Fix**:
1. Verify `APP_PAYMENT_RAZORPAY_KEY_ID` and `APP_PAYMENT_RAZORPAY_KEY_SECRET`
2. Ensure test keys for development, live keys for production
3. Keys must be from same account (test pair or live pair)

### Rate limiting blocks requests
**Cause**: IP-based rate limiting too strict
**Fix**: Increase `APP_RATE_LIMIT_RPM` or disable rate limiting for testing
