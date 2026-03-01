-- V10: Sick Leave, Business Trip, and Project/Time Allocation Tables

CREATE TABLE sick_leaves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REPORTED' CHECK (status IN ('REPORTED', 'CERTIFICATE_PENDING', 'CERTIFICATE_RECEIVED', 'CANCELLED')),
    has_certificate BOOLEAN NOT NULL DEFAULT FALSE,
    certificate_submitted_at TIMESTAMPTZ,
    notes TEXT,
    reported_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE business_trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    destination VARCHAR(500) NOT NULL,
    purpose VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    rejection_reason TEXT,
    notes TEXT,
    estimated_cost DECIMAL(10, 2),
    actual_cost DECIMAL(10, 2),
    cost_center VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    cost_center VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE time_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    minutes INT NOT NULL CHECK (minutes > 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for sick_leaves
CREATE INDEX idx_sick_leaves_user_id ON sick_leaves(user_id);
CREATE INDEX idx_sick_leaves_status ON sick_leaves(status);
CREATE INDEX idx_sick_leaves_dates ON sick_leaves(start_date, end_date);

-- Indexes for business_trips
CREATE INDEX idx_business_trips_user_id ON business_trips(user_id);
CREATE INDEX idx_business_trips_status ON business_trips(status);
CREATE INDEX idx_business_trips_dates ON business_trips(start_date, end_date);

-- Indexes for projects
CREATE INDEX idx_projects_code ON projects(code);
CREATE INDEX idx_projects_is_active ON projects(is_active);

-- Indexes for time_allocations
CREATE INDEX idx_time_allocations_user_id ON time_allocations(user_id);
CREATE INDEX idx_time_allocations_project_id ON time_allocations(project_id);
CREATE INDEX idx_time_allocations_date ON time_allocations(date);
CREATE INDEX idx_time_allocations_user_date ON time_allocations(user_id, date);
