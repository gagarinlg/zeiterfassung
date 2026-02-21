# Quick Start Guide

## Prerequisites

- **Docker** 24.0+ — [Install Docker](https://docs.docker.com/get-docker/)
- **Docker Compose** v2+ (included with Docker Desktop)
- **4 GB RAM** minimum, 8 GB recommended
- **Ports available:** 80, 443, 8080, 5432

## 1. Clone Repository

```bash
git clone https://github.com/gagarinlg/zeiterfassung.git
cd zeiterfassung
```

## 2. Configure Environment

```bash
cp .env.example .env
```

Edit `.env` and set these required values:

```bash
# Strong database password (min 16 characters)
DB_PASSWORD=your-strong-database-password

# JWT secret — MUST be a random 64-byte base64 string
# Generate with: openssl rand -base64 64
JWT_SECRET=your-jwt-secret-here

# Your domain (used for CORS)
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

## 3. SSL Certificates (for HTTPS)

For local development with self-signed certificates:

```bash
mkdir -p docker/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout docker/certs/key.pem \
  -out docker/certs/cert.pem \
  -subj "/C=DE/ST=Bayern/L=München/O=Zeiterfassung/CN=localhost"
```

For production, replace with certificates from Let's Encrypt or your CA.

## 4. Start Services

```bash
docker compose up -d
```

Monitor startup:
```bash
docker compose logs -f
```

Wait until you see `Started ZeiterfassungApplication` in the backend logs.

## 5. First Login

The system does not create a default admin user automatically for security reasons.
Run the following command to create the initial super admin:

```bash
docker compose exec backend java -jar app.jar --create-admin \
  --email=admin@example.com \
  --password=change-me-immediately
```

Then log in at `https://localhost` and **immediately change the password**.

## 6. Verify Installation

- **Web UI:** https://localhost
- **API Health:** https://localhost/api/actuator/health
- **API Docs:** https://localhost/api/swagger-ui.html

## Stopping Services

```bash
docker compose down          # Stop and remove containers
docker compose down -v       # Also remove database volume (data loss!)
```

## Updating

```bash
git pull origin main
docker compose build --no-cache
docker compose up -d
```
