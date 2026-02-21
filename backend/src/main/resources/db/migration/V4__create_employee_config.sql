-- V4: Employee Configuration Table

CREATE TABLE employee_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    weekly_work_hours DECIMAL(5, 2) NOT NULL DEFAULT 40.00,
    daily_work_hours DECIMAL(5, 2) NOT NULL DEFAULT 8.00,
    work_days JSONB NOT NULL DEFAULT '[1,2,3,4,5]',
    vacation_days_per_year INT NOT NULL DEFAULT 30,
    vacation_carry_over_max INT NOT NULL DEFAULT 10,
    contract_start_date DATE,
    contract_end_date DATE,
    is_home_office_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index
CREATE INDEX idx_employee_configs_user_id ON employee_configs(user_id);
