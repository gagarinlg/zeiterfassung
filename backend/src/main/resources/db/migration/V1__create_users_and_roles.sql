-- V1: Users, Roles, Permissions
-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Permissions table
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Role-Permissions junction table
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    employee_number VARCHAR(50) UNIQUE,
    rfid_tag_id VARCHAR(100) UNIQUE,
    phone VARCHAR(50),
    photo_url VARCHAR(500),
    manager_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- User-Roles junction table
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_manager_id ON users(manager_id);
CREATE INDEX idx_users_rfid_tag_id ON users(rfid_tag_id);
CREATE INDEX idx_users_is_deleted ON users(is_deleted);

-- Seed default roles
INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', 'Full system access, can manage all settings, users, and data'),
    ('ADMIN', 'Can manage users, settings, and view all reports'),
    ('MANAGER', 'Can approve vacation, correct time entries for their team, view team reports'),
    ('EMPLOYEE', 'Can track own time, request vacation, view own data');

-- Seed default permissions
INSERT INTO permissions (name, description) VALUES
    ('time.track.own', 'Track own time entries'),
    ('time.edit.own', 'Edit own time entries'),
    ('time.edit.team', 'Edit team time entries'),
    ('time.view.own', 'View own time entries'),
    ('time.view.team', 'View team time entries'),
    ('time.view.all', 'View all time entries'),
    ('vacation.request.own', 'Request own vacation'),
    ('vacation.approve', 'Approve vacation requests'),
    ('vacation.view.team', 'View team vacation'),
    ('vacation.view.all', 'View all vacation requests'),
    ('admin.users.manage', 'Manage users'),
    ('admin.settings.manage', 'Manage system settings'),
    ('admin.reports.view', 'View all reports'),
    ('csv.import', 'Import CSV data'),
    ('csv.export', 'Export data as CSV');

-- Assign permissions to roles
-- EMPLOYEE: basic time tracking and vacation
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'EMPLOYEE'
  AND p.name IN ('time.track.own', 'time.edit.own', 'time.view.own', 'vacation.request.own');

-- MANAGER: employee permissions + team management
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MANAGER'
  AND p.name IN ('time.track.own', 'time.edit.own', 'time.view.own', 'vacation.request.own',
                 'time.edit.team', 'time.view.team', 'vacation.approve', 'vacation.view.team',
                 'admin.reports.view', 'csv.export');

-- ADMIN: all except super admin exclusive
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('time.track.own', 'time.edit.own', 'time.view.own', 'vacation.request.own',
                 'time.edit.team', 'time.view.team', 'time.view.all', 'vacation.approve',
                 'vacation.view.team', 'vacation.view.all', 'admin.users.manage',
                 'admin.settings.manage', 'admin.reports.view', 'csv.import', 'csv.export');

-- SUPER_ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN';
