# M-Pesa Product Store

A simple product-selling website with:
- Vue 3 + Vite frontend
- Spring Boot backend
- M-Pesa Daraja STK Push
- M-Pesa callback endpoint
- Simple in-memory products/orders for easy testing

## Requirements

- Java 17+
- Maven 3.9+
- Node.js 18+
- An accessible callback URL when testing real Daraja callbacks

## 1. Configure M-Pesa

Open:

`backend/src/main/resources/application.properties`

Set your Daraja credentials:

```properties
mpesa.consumer-key=YOUR_CONSUMER_KEY
mpesa.consumer-secret=YOUR_CONSUMER_SECRET
mpesa.shortcode=YOUR_SHORTCODE
mpesa.passkey=YOUR_PASSKEY
mpesa.callback-url=https://YOUR_PUBLIC_URL/api/payments/mpesa/callback
mpesa.environment=sandbox
```

Do not put these credentials in the Vue frontend.

For local callback testing, expose port 8080 with a tunnel such as ngrok or Cloudflare Tunnel, then use the HTTPS public URL as `mpesa.callback-url`.

## 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

Backend:

`http://localhost:8080`

## 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend:

`http://localhost:5173`

## API

### Products

`GET /api/products`

### Start STK Push

`POST /api/payments/mpesa/stk-push`

Example:

```json
{
  "phoneNumber": "254712345678",
  "amount": 100,
  "productName": "Wireless Headphones"
}
```

### M-Pesa callback

`POST /api/payments/mpesa/callback`

## Phone numbers

The frontend accepts common Kenyan formats such as:

- `0712345678`
- `0112345678`
- `254712345678`
- `+254712345678`

They are normalized before being sent to Daraja.

## Important

This project is intentionally simple and uses in-memory data. Before production, add a database, authentication/admin management, persistent orders, inventory management, payment reconciliation, validation, logging, HTTPS, and secure secret management.
