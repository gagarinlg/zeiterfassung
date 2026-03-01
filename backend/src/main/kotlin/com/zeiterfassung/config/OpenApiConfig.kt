package com.zeiterfassung.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Zeiterfassung API",
        description = "Time tracking system compliant with German labor law (ArbZG). " +
            "Provides endpoints for authentication, time tracking, vacation management, " +
            "user administration, terminal RFID scanning, and employee configuration.",
        version = "1.0.0",
        contact = Contact(name = "Zeiterfassung Team"),
    ),
    tags = [
        Tag(name = "Authentication", description = "Login, logout, token refresh, TOTP 2FA, and password reset"),
        Tag(name = "Time Tracking", description = "Clock in/out, breaks, daily summaries, timesheets, and CSV export"),
        Tag(name = "Vacation", description = "Vacation requests, approvals, balances, public holidays, and team calendar"),
        Tag(name = "Users", description = "User CRUD, role assignment, RFID management, and team hierarchy"),
        Tag(name = "Admin", description = "Audit log, system settings, LDAP configuration, and test email"),
        Tag(name = "Terminal", description = "RFID terminal scan and heartbeat endpoints"),
        Tag(name = "Employee Config", description = "Employee work configuration management"),
    ],
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT access token. Obtain via POST /api/auth/login",
)
class OpenApiConfig
