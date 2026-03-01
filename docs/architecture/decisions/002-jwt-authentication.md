# ADR 002: JWT-Based Authentication

## Status

Accepted

## Context

The system requires secure authentication across a web frontend, mobile apps (Android/iOS), and Raspberry Pi terminals. Session-based authentication is impractical for mobile and terminal clients that may operate intermittently.

## Decision

We use **JWT (JSON Web Tokens)** with short-lived access tokens (15 minutes) and long-lived refresh tokens (7 days) with rotation and reuse detection.

### Design

- **Access tokens**: HS256-signed, 15-minute expiry, stateless validation via filter.
- **Refresh tokens**: Stored as hashed values in the database; rotated on each use; reuse of an old token revokes the entire family (reuse detection).
- **Account lockout**: After 5 failed login attempts, the account is locked for 15 minutes.
- **TOTP 2FA**: Optional two-factor authentication via RFC 6238 TOTP codes.

## Consequences

- Stateless access token validation reduces database load per request.
- Refresh token rotation limits the window of a compromised token.
- Reuse detection protects against token theft by invalidating the entire token chain.
- Mobile and terminal clients can store tokens locally and refresh transparently.
