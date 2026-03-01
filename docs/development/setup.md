# Development Environment Setup

> See also: **[Testing Guide](testing.md)** — Unit tests, E2E tests, coverage, and CI/CD integration for all components.

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| JDK | 21+ | Backend (Spring Boot) |
| Node.js | 20+ | Frontend (React) |
| Rust | stable | Terminal (Raspberry Pi) |
| Docker | 24+ | Database (PostgreSQL) |
| Git | any | Version control |

## Backend Setup

### 1. Start PostgreSQL

```bash
docker run -d \
  --name zeiterfassung-db \
  -e POSTGRES_DB=zeiterfassung \
  -e POSTGRES_USER=zeiterfassung \
  -e POSTGRES_PASSWORD=zeiterfassung \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Configure Backend

The `development` profile uses these defaults (no `.env` needed for local dev):
- DB URL: `jdbc:postgresql://localhost:5432/zeiterfassung`
- DB User: `zeiterfassung`
- DB Password: `zeiterfassung`

### 3. Run Backend

```bash
cd backend
chmod +x gradlew
./gradlew bootRun --args='--spring.profiles.active=development'
```

Backend starts at `http://localhost:8080`
API docs at `http://localhost:8080/api/swagger-ui.html`

### 4. Run Backend Tests

```bash
cd backend
./gradlew test
./gradlew test jacocoTestReport  # With coverage
```

## Frontend Setup

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Run Development Server

```bash
npm run dev
```

Frontend starts at `http://localhost:5173` with HMR enabled.
API requests to `/api/*` are proxied to `http://localhost:8080`.

### 3. Run Tests

```bash
npm test               # Watch mode
npm run test:coverage  # With coverage
npm run test:e2e       # Playwright E2E tests
```

### 4. Linting

```bash
npm run lint           # Check
npm run lint:fix       # Auto-fix
npm run format         # Prettier
```

## Terminal Setup

### 1. Install Rust

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source ~/.cargo/env
```

### 2. Install System Dependencies (Linux)

```bash
sudo apt-get install -y libasound2-dev libssl-dev pkg-config
```

### 3. Run Terminal

```bash
cd terminal
CONFIG_PATH=terminal.toml cargo run
```

### 4. Run Tests

```bash
cd terminal
cargo test
cargo clippy -- -D warnings
```

## Code Style

- **Backend (Kotlin):** `./gradlew ktlintCheck` / `./gradlew ktlintFormat`
- **Frontend (TypeScript):** `npm run lint` / `npm run format`
- **Terminal (Rust):** `cargo fmt` / `cargo clippy`
- **All:** Configured in `.editorconfig`

## IDE Setup

### IntelliJ IDEA (recommended for backend)
1. Open `backend/` as a Gradle project
2. Enable ktlint plugin
3. Set code style from `.editorconfig`

### VS Code (recommended for frontend)
Install extensions: ESLint, Prettier, Tailwind CSS IntelliSense

### RustRover / VS Code with rust-analyzer (terminal)
Use `rust-analyzer` extension for VS Code
