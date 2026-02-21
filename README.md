# Zeiterfassung ⏱️

[![CI](https://github.com/gagarinlg/zeiterfassung/actions/workflows/ci.yml/badge.svg)](https://github.com/gagarinlg/zeiterfassung/actions/workflows/ci.yml)
[![Security](https://github.com/gagarinlg/zeiterfassung/actions/workflows/security.yml/badge.svg)](https://github.com/gagarinlg/zeiterfassung/actions/workflows/security.yml)

A comprehensive time recording (time tracking) system for employees, built to comply with **German labor law** (Arbeitszeitgesetz – ArbZG).

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Clients                             │
│  ┌───────────┐  ┌───────────┐  ┌────────────────────────┐  │
│  │  Browser  │  │ Android / │  │  Raspberry Pi Terminal │  │
│  │  (React)  │  │   iOS App │  │  (Rust + NFC/RFID)     │  │
│  └─────┬─────┘  └─────┬─────┘  └──────────┬─────────────┘  │
└────────┼──────────────┼──────────────────┬─┘               │
         │              │                  │                  │
         ▼              ▼                  ▼                  │
┌─────────────────────────────────────────────────────────────┐
│                    REST API (HTTPS)                         │
│              Spring Boot 3 + Kotlin Backend                 │
│         JWT Auth | RBAC | ArbZG Compliance Logic           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │   PostgreSQL Database  │
              │   Flyway Migrations    │
              └────────────────────────┘
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Backend | Kotlin + Spring Boot 3, Spring Security, JPA, Flyway |
| Database | PostgreSQL 16 |
| Frontend | React 18, TypeScript, Vite, TailwindCSS |
| Android | Kotlin + Jetpack Compose, Hilt, Retrofit |
| iOS | Swift + SwiftUI, Combine |
| Terminal | Rust + iced, reqwest, SQLite |
| Auth | JWT (jjwt), bcrypt, refresh token rotation |
| CI/CD | GitHub Actions |

## Quick Start with Docker Compose

### Prerequisites

- Docker 24+ and Docker Compose v2+
- 4 GB RAM minimum

### Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/gagarinlg/zeiterfassung.git
   cd zeiterfassung
   ```

2. **Configure environment:**
   ```bash
   cp .env.example .env
   # Edit .env and set strong values for DB_PASSWORD and JWT_SECRET
   ```

3. **Start all services:**
   ```bash
   docker compose up -d
   ```

4. **Access the application:**
   - Web UI: https://localhost (or http://localhost:8080/api for backend API directly)
   - API Docs: http://localhost:8080/api/swagger-ui.html

### Generate a strong JWT secret:
```bash
openssl rand -base64 64
```

## Development

See [docs/development/setup.md](docs/development/setup.md) for full development environment setup.

### Run backend locally:
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=development'
```

### Run frontend locally:
```bash
cd frontend
npm install
npm run dev
```

### Run terminal locally:
```bash
cd terminal
cargo run
```

## Documentation

- [Installation Guide](docs/installation/quick-start.md)
- [Development Setup](docs/development/setup.md)
- [Architecture](docs/development/architecture.md)
- [Adding Languages](docs/development/adding-languages.md)

## German Labor Law Compliance (ArbZG)

The system enforces the following rules:
- ✅ Maximum 10 hours of work per day (§3 ArbZG)
- ✅ Mandatory 30-minute break after 6 hours (§4 ArbZG)
- ✅ Mandatory 45-minute break after 9 hours (§4 ArbZG)
- ✅ Minimum 11 hours rest between working days (§5 ArbZG)
- ✅ No work on Sundays and public holidays (§9 ArbZG)

## Contributing

Please read our [Contributing Guide](docs/development/setup.md) and follow the code style guidelines defined in `.editorconfig` and component-specific linting configs.

## License
GPLv3 license
