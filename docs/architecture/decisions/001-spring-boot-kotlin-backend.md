# ADR 001: Spring Boot with Kotlin for Backend

## Status

Accepted

## Context

We need a backend framework for a time tracking system that handles user authentication, time tracking, vacation management, and compliance with German labor law (ArbZG). The system must support RESTful APIs, database access via JPA, email notifications, and role-based access control.

## Decision

We chose **Spring Boot 3.2 with Kotlin** as the backend technology stack.

### Key reasons

- **Spring Boot ecosystem**: Mature ecosystem with Spring Security, Spring Data JPA, Spring Mail, and Flyway migrations out of the box.
- **Kotlin**: Concise syntax, null safety, data classes for DTOs, and full interoperability with the Java ecosystem.
- **JPA/Hibernate**: Standard ORM for PostgreSQL with Flyway for version-controlled database migrations.
- **Spring Security**: Built-in support for method-level security (`@PreAuthorize`), CORS, CSRF, and session management.
- **Production readiness**: Spring Boot Actuator for health checks and metrics; broad hosting support.

## Consequences

- Developers need Kotlin/Spring Boot experience.
- JVM runtime requires more memory than lightweight alternatives (Go, Rust), acceptable for a server deployment.
- Access to the full Java library ecosystem (e.g., `jjwt` for JWT, `commons-codec` for TOTP).
