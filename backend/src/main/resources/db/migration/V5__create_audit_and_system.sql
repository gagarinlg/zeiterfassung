-- V5: Audit Log, System Settings, Refresh Tokens

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(200) NOT NULL UNIQUE,
    value TEXT,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Seed default system settings
INSERT INTO system_settings (key, value, description) VALUES
    ('company.name', 'Zeiterfassung GmbH', 'Company name'),
    ('company.timezone', 'Europe/Berlin', 'Default timezone for time calculations'),
    ('working.hours.default', '8', 'Default daily working hours'),
    ('break.mandatory.after.hours', '6', 'Mandatory break required after N hours (ArbZG §4)'),
    ('break.duration.minutes.6h', '30', 'Break duration in minutes after 6 hours'),
    ('break.duration.minutes.9h', '45', 'Break duration in minutes after 9 hours'),
    ('max.daily.hours', '10', 'Maximum allowed daily working hours (ArbZG §3)'),
    ('min.rest.hours', '11', 'Minimum rest period between working days (ArbZG §5)'),
    ('account.lockout.attempts', '5', 'Number of failed login attempts before account lockout'),
    ('account.lockout.minutes', '30', 'Account lockout duration in minutes');
