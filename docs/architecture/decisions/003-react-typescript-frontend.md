# ADR 003: React with TypeScript for Frontend

## Status

Accepted

## Context

The admin panel and employee self-service portal require a modern, responsive single-page application. The UI must support internationalization (German/English), role-based navigation, and real-time status displays.

## Decision

We use **React 18 with TypeScript**, built with **Vite**, styled with **TailwindCSS**.

### Key libraries

- **react-router-dom**: Client-side routing with protected routes.
- **react-i18next**: Internationalization with JSON translation files.
- **@tanstack/react-query**: Server state management with caching and automatic refetching.
- **axios**: HTTP client with interceptor-based JWT refresh.
- **date-fns**: Locale-aware date formatting (German/English).
- **zod**: Runtime validation of form inputs.

### Testing

- **Vitest + React Testing Library**: Unit tests for components and utilities.
- **Playwright**: End-to-end tests across Chromium, Firefox, and WebKit.

## Consequences

- TypeScript catches type errors at compile time, reducing runtime bugs.
- TailwindCSS utility classes keep the CSS footprint small and consistent.
- Vite provides fast HMR for development and optimized production builds.
- React ecosystem has abundant community support and component libraries.
