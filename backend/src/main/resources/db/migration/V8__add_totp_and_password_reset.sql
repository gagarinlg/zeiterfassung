-- TOTP 2FA support
ALTER TABLE users ADD COLUMN totp_secret VARCHAR(64);
ALTER TABLE users ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);

-- LDAP configuration settings
INSERT INTO system_settings (key, value, description) VALUES
    ('ldap.enabled', 'false', 'Enable LDAP authentication'),
    ('ldap.url', '', 'LDAP server URL (e.g. ldap://dc.example.com:389)'),
    ('ldap.base-dn', '', 'LDAP base DN (e.g. dc=example,dc=com)'),
    ('ldap.user-search-base', 'ou=Users', 'User search base relative to base DN'),
    ('ldap.user-search-filter', '(sAMAccountName={0})', 'LDAP user search filter'),
    ('ldap.group-search-base', 'ou=Groups', 'Group search base relative to base DN'),
    ('ldap.group-search-filter', '(member={0})', 'LDAP group search filter'),
    ('ldap.manager-dn', '', 'LDAP bind DN for searches'),
    ('ldap.manager-password', '', 'LDAP bind password'),
    ('ldap.active-directory-mode', 'false', 'Use Active Directory mode (simplified config)'),
    ('ldap.active-directory-domain', '', 'Active Directory domain (e.g. example.com)'),
    ('ldap.role-mapping', '{}', 'JSON mapping of LDAP groups to app roles'),
    ('ldap.email-attribute', 'mail', 'LDAP attribute for email'),
    ('ldap.first-name-attribute', 'givenName', 'LDAP attribute for first name'),
    ('ldap.last-name-attribute', 'sn', 'LDAP attribute for last name'),
    ('ldap.employee-number-attribute', 'employeeNumber', 'LDAP attribute for employee number')
ON CONFLICT (key) DO NOTHING;
