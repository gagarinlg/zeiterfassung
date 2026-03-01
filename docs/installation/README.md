# Installation Guide

## Table of Contents
- [System Requirements](#system-requirements)
- [Quick Start (Docker)](#quick-start-docker)
- [Development Setup](#development-setup)
- [Production Deployment](#production-deployment)
- [Raspberry Pi Terminal Setup](#raspberry-pi-terminal-setup)
- [Mobile Apps](#mobile-apps)
- [Configuration Reference](#configuration-reference)
- [Troubleshooting](#troubleshooting)

### Detailed Guides
- **[Mobile Apps — Full Installation & Deployment Guide](mobile-apps.md)** — Building, signing, distribution, and MDM provisioning for Android and iOS
- **[Mobile Provisioning & MDM Configuration](mobile-provisioning.md)** — Enterprise MDM setup for Google Workspace, Intune, Jamf, Workspace ONE
- **[Raspberry Pi Terminal — Full Installation Guide](terminal.md)** — Hardware setup, OS configuration, kiosk mode, RFID, systemd service, offline buffering

## System Requirements

### Docker Deployment
- Docker 24.0+ with Docker Compose v2
- 4 GB RAM minimum (8 GB recommended)
- 10 GB disk space
- Ports: 80, 443 (web), 8080 (API), 5432 (PostgreSQL)

### Development
- JDK 21 (Temurin recommended)
- Node.js 20+ with npm
- PostgreSQL 16+ (or Docker)
- Rust toolchain (for terminal app)
- Android Studio (for Android app)
- Xcode 15+ (for iOS app, macOS only)

## Quick Start (Docker)

### 1. Clone & Configure
```bash
git clone https://github.com/gagarinlg/zeiterfassung.git
cd zeiterfassung
cp .env.example .env
```

Edit `.env`:
```bash
# Strong database password (min 16 characters)
DB_PASSWORD=<strong-random-password>

# JWT secret — MUST be a random 64-byte base64 string
JWT_SECRET=$(openssl rand -base64 64)

# Your domain (used for CORS)
CORS_ALLOWED_ORIGINS=https://your-domain.com

# Initial admin password (change after first login)
SEED_ADMIN_PASSWORD=<initial-admin-password>
```

### 2. SSL Certificates (for HTTPS)

For local development with self-signed certificates:

```bash
mkdir -p docker/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout docker/certs/key.pem \
  -out docker/certs/cert.pem \
  -subj "/C=DE/ST=Bayern/L=München/O=Zeiterfassung/CN=localhost"
```

For production, replace with certificates from Let's Encrypt or your CA.

### 3. Start Services
```bash
docker compose up -d
docker compose logs -f  # Wait for "Started ZeiterfassungApplication"
```

### 4. First Login
Navigate to `https://localhost`. Login with:
- Email: `admin@zeiterfassung.local`
- Password: value of `SEED_ADMIN_PASSWORD` (default: `Admin@123!`)

**Change the admin password immediately after first login.**

### 5. Verify Installation

- **Web UI:** https://localhost
- **API Health:** https://localhost/api/actuator/health
- **API Docs:** https://localhost/api/swagger-ui.html

### Stopping Services

```bash
docker compose down          # Stop and remove containers
docker compose down -v       # Also remove database volume (data loss!)
```

### Updating

```bash
git pull origin main
docker compose build --no-cache
docker compose up -d
```

## Development Setup

### Backend
```bash
cd backend

# Start PostgreSQL (via Docker)
docker compose up -d db

# Run the backend
./gradlew bootRun
# Backend available at http://localhost:8080/api
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# Frontend available at http://localhost:5173
# API requests are proxied to http://localhost:8080
```

### Running Tests
```bash
# Backend tests
cd backend && ./gradlew test

# Frontend unit tests
cd frontend && npm run test:coverage

# Frontend E2E tests
cd frontend && npx playwright install --with-deps chromium
cd frontend && npx playwright test --config=e2e/playwright.config.ts

# Terminal tests
cd terminal && cargo test
```

## Production Deployment

### Environment Variables Reference
| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/zeiterfassung` |
| `DB_USERNAME` | Database username | `zeiterfassung` |
| `DB_PASSWORD` | Database password | `zeiterfassung` |
| `JWT_SECRET` | JWT signing secret (min 256 bits) | (insecure default) |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins (comma-separated) | `http://localhost:5173,http://localhost:3000` |
| `SEED_ADMIN_PASSWORD` | Initial admin password | `Admin@123!` |
| `MAIL_HOST` | SMTP server hostname | `localhost` |
| `MAIL_PORT` | SMTP server port | `587` |
| `MAIL_USERNAME` | SMTP username | (empty) |
| `MAIL_PASSWORD` | SMTP password | (empty) |
| `MAIL_ENABLED` | Enable email sending | `true` |
| `SERVER_PORT` | Backend HTTP port | `8080` |

### SSL/TLS Setup
For production, use Let's Encrypt:
```bash
certbot certonly --standalone -d your-domain.com
```
Place certificates in `docker/certs/` and update `docker-compose.yml`.

### Database Backups
```bash
# Create backup
docker compose exec db pg_dump -U zeiterfassung zeiterfassung > backup_$(date +%Y%m%d).sql

# Restore backup
docker compose exec -T db psql -U zeiterfassung zeiterfassung < backup.sql
```

## Raspberry Pi Terminal Setup

### Hardware Requirements
- Raspberry Pi 5 (4+ GB RAM)
- Compatible display (any resolution, configurable)
- USB RFID/NFC reader (any USB HID keyboard-emulating reader)
- Speaker (optional, for audio feedback)

### Installation
```bash
# Copy the terminal binary to the Pi
scp terminal/target/release/zeiterfassung-terminal pi@raspberrypi:~/

# Copy configuration
scp terminal/terminal.toml pi@raspberrypi:~/

# Edit configuration
ssh pi@raspberrypi
nano terminal.toml  # Set API URL, display settings, terminal_id
```

### Configuration (terminal.toml)
See `terminal/terminal.toml` for all available options including:
- Display resolution, orientation, theme
- API endpoint and authentication
- RFID reader settings
- Audio feedback configuration
- Offline buffering settings

## Mobile Apps

### Android
Build the APK:
```bash
cd mobile/android
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

### iOS
Open in Xcode:
```bash
cd mobile/ios
open ZeiterfassungApp.xcodeproj
```

## Configuration

### Date & Time Format
The system supports configurable date and time formats:
- **Global defaults**: Set via Admin → System Settings (`display.date_format`, `display.time_format`, `display.first_day_of_week`)
- **Per-user overrides**: Each user can set their preferred format in their profile
- **Supported date formats**: `DD.MM.YYYY` (German), `YYYY-MM-DD` (ISO), `MM/DD/YYYY` (US)
- **Supported time formats**: `24h`, `12h`

### Internationalization
The application supports German (primary) and English. Set the language via the language switcher in the UI.

## Troubleshooting

### Common Issues

**"Invalid CORS request" error**
Ensure `CORS_ALLOWED_ORIGINS` includes your frontend URL (e.g., `http://localhost:5173` for development).

**Database connection failed**
Check that PostgreSQL is running and `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` are correct.

**Backend won't start with test profile**
The test profile uses H2 in-memory database which is only available as a test dependency. Use the development profile for local running.

**Frontend API calls fail**
In development, ensure the Vite dev server is running (`npm run dev`). The proxy forwards `/api` requests to the backend.

**Terminal cannot connect to backend**
Verify the `base_url` in `terminal.toml` is correct and reachable from the Raspberry Pi. Check firewall rules and SSL certificate validity.

**RFID reader not detected**
Ensure the reader is plugged in and recognized as a USB HID device. Check `input_device` in `terminal.toml` — use `"auto"` for automatic detection or specify the device path (e.g., `/dev/input/event0`).
