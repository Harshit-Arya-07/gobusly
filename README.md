# Bus Booking System

A full-stack bus booking platform with a Spring Boot + MongoDB backend and a React + Vite frontend. The app supports bus search, seat selection, bookings, Razorpay payments, payment history, admin bus management, admin booking management, user authentication, email notifications after successful payment, and role-based access control.

## Features

### User features
- Search buses by source, destination, and travel date
- View available seats for a selected bus
- Select seats and proceed to payment
- Pay through Razorpay checkout
- Receive payment success email with payment details and bus details
- View booking history
- View payment history with status, order ID, payment ID, receipt, amount, time, and failure reason
- Cancel confirmed bookings
- Login with password visibility toggle
- Optional `Remember me for 10 minutes` login session

### Admin features
- Create, update, and delete buses
- Set per-seat bus fare in rupees
- View all bookings
- Cancel bookings from the admin dashboard
- View full payment ledger with all payment attempts
- Prevent admins from booking buses
- Prevent one admin from deleting buses created by another admin

### Platform behavior
- JWT-based authentication
- Role-based route protection
- Responsive UI for mobile and desktop
- Razorpay payment order creation and verification
- Email notification on successful payment
- Bus-seat synchronization when bus seat count changes

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.3.x
- Spring Web
- Spring Security
- Spring Data MongoDB
- Spring Validation
- Spring Mail
- Razorpay Java SDK
- JWT authentication

### Frontend
- React 18
- Vite
- React Router
- Axios
- Tailwind CSS toolchain available for utility-based styling
- Custom responsive CSS styling

### Database
- MongoDB

## Project Structure

```text
Bus Booking system/
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/busbooking/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── exception/
│   │   │   │   ├── repository/
│   │   │   │   ├── security/
│   │   │   │   └── service/
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── ...
│   └── target/
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── utils/
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── package.json
│   ├── vite.config.js
│   └── ...
└── README.md
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- MongoDB instance or MongoDB Atlas connection
- Razorpay account for payment gateway
- Gmail account with an App Password for email notifications

## Setup

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd "Bus Booking system"
```

### 2. Backend configuration

Update `backend/src/main/resources/application.properties` with your own values.

Required properties:

```properties
spring.data.mongodb.uri=your_mongodb_connection_string
app.jwt.secret=your_jwt_secret
app.jwt.expiration-ms=86400000

app.payment.razorpay.key-id=your_razorpay_key_id
app.payment.razorpay.key-secret=your_razorpay_key_secret

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_gmail@gmail.com
spring.mail.password=your_gmail_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.from=your_gmail@gmail.com
```

### 3. Frontend configuration

The frontend uses the backend at:

```text
http://localhost:8080/api
```

If your backend runs on a different host or port, update `frontend/src/services/api.js`.

## Run the project

### Backend

From the `backend` folder:

```bash
cd backend
mvn spring-boot:run
```

Or build and run the packaged JAR:

```bash
cd backend
mvn clean package
java -jar target/gobusly-0.0.1-SNAPSHOT.jar
```

### Frontend

From the `frontend` folder:

```bash
cd frontend
npm install
npm run dev
```

For production build:

```bash
cd frontend
npm run build
npm run preview
```

## Deployment (Short)

### Backend (Render - Docker)

- Language: `Docker`
- Branch: `main`
- Root Directory: `backend`
- Dockerfile Path: `Dockerfile`

Important: If Root Directory is `backend`, then Dockerfile Path must be only `Dockerfile` (not `backend/Dockerfile`).

Add these Environment Variables in Render:

- `SPRING_DATA_MONGODB_URI`
- `APP_JWT_SECRET`
- `APP_JWT_EXPIRATION_MS=86400000`
- `APP_PAYMENT_RAZORPAY_KEY_ID`
- `APP_PAYMENT_RAZORPAY_KEY_SECRET`
- `SPRING_MAIL_HOST=smtp.gmail.com`
- `SPRING_MAIL_PORT=587`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`
- `APP_MAIL_FROM`
- `FRONTEND_URL=https://<your-vercel-domain>`

### Frontend (Vercel)

- Framework: `Vite`
- Root Directory: `frontend`
- Build Command: `npm run build`
- Output Directory: `dist`

Add this Environment Variable in Vercel:

- `VITE_API_BASE_URL=https://<your-render-backend-domain>/api`

### CORS Note

Allow your Vercel frontend domain in backend CORS config (`SecurityConfig`) before production use.

## Main UI pages

- `/` Home page with bus search
- `/buses` Bus listing
- `/seats/:busId` Seat selection
- `/booking` Booking confirmation and payment flow
- `/my-bookings` User bookings and payment history
- `/login` Login page
- `/register` Register page
- `/admin/buses` Admin dashboard for buses, bookings, and payment ledger

## Authentication

- JWT token is issued after login or registration
- Role-based UI and route protection are enforced on the frontend
- Backend also enforces access control for protected operations
- Login supports:
  - password visibility toggle
  - `Remember me for 10 minutes` option

### Storage behavior
- If `Remember me` is enabled, auth is stored in `localStorage` and auto-expires after 10 minutes
- If disabled, auth is stored in `sessionStorage` only

## Booking flow

1. User searches a bus
2. User selects a bus and chooses seats
3. User reviews the booking summary
4. Razorpay order is created using the bus fare per seat
5. Payment is verified after checkout
6. Booking is created and stored
7. Payment success email is sent to the user
8. Booking and payment history become visible in the user dashboard

## Admin flow

- Admin can create buses and set fare in rupees per seat
- Admin can update bus details and total seats
- Bus seat records are synchronized when seat count changes
- Admin can view all bookings
- Admin can cancel bookings
- Admin can view the full payment ledger
- Admins cannot create bookings
- One admin cannot delete another admin’s buses

## Payment ledger

Every payment attempt is stored with:
- user ID
- bus ID
- selected seats
- amount
- order ID
- payment ID
- receipt
- status
- failure reason
- created timestamp
- updated timestamp

Payment statuses:
- `PENDING`
- `SUCCESS`
- `FAILED`

## Email notifications

On successful payment, the system sends a Gmail notification containing:
- payment amount
- order ID
- payment ID
- receipt
- payment status
- timestamp
- bus number
- route
- departure time
- selected seats

### Gmail requirement

Use a Gmail App Password, not your normal account password.

## Important notes

- Do not commit real secrets into `application.properties`
- Replace placeholder credentials before running the app in production
- MongoDB Atlas IP allowlist must include your machine or server IP
- Razorpay test keys should be used in development
- If a bus was created before the `createdByAdminId` field existed, deletion is restricted unless ownership data is present

## API overview

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`

### Buses
- `GET /api/buses`
- `GET /api/buses/search?source=&destination=&date=`
- `POST /api/buses`
- `PUT /api/buses/{id}`
- `DELETE /api/buses/{id}`

### Seats
- `GET /api/seats/{busId}`

### Bookings
- `POST /api/bookings`
- `GET /api/bookings/user/{userId}`
- `GET /api/bookings` (admin only)
- `DELETE /api/bookings/{id}`

### Payments
- `POST /api/payments/order`
- `POST /api/payments/verify`
- `POST /api/payments/fail`
- `GET /api/payments/user/{userId}`
- `GET /api/payments/admin` (admin only)

## Common troubleshooting

### Frontend does not start
- Ensure you are inside the `frontend` folder before running `npm run dev`
- Install dependencies with `npm install`

### Payment fails with Razorpay errors
- Verify `app.payment.razorpay.key-id` and `app.payment.razorpay.key-secret`
- Make sure the receipt length remains within Razorpay limits

### Email is not sending
- Check Gmail App Password
- Confirm SMTP values in `application.properties`
- Make sure 2-step verification is enabled on the Gmail account

### Admin cannot delete a bus
- The bus must have been created by the same admin
- Legacy buses without ownership data are intentionally blocked from deletion

## License

No license has been specified for this project.

## Status

The project is actively evolving and currently includes:
- user booking flow
- admin dashboard
- payment processing
- payment ledger
- email notifications
- role-based access control
