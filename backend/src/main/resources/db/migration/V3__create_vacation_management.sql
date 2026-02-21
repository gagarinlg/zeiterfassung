-- V3: Vacation Management Tables

CREATE TABLE vacation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_half_day_start BOOLEAN NOT NULL DEFAULT FALSE,
    is_half_day_end BOOLEAN NOT NULL DEFAULT FALSE,
    total_days DECIMAL(5, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    rejection_reason TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE vacation_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    year INT NOT NULL,
    total_days DECIMAL(5, 2) NOT NULL DEFAULT 0,
    used_days DECIMAL(5, 2) NOT NULL DEFAULT 0,
    carried_over_days DECIMAL(5, 2) NOT NULL DEFAULT 0,
    remaining_days DECIMAL(5, 2) GENERATED ALWAYS AS (total_days + carried_over_days - used_days) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, year)
);

CREATE TABLE public_holidays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    name VARCHAR(200) NOT NULL,
    state_code VARCHAR(10),
    is_recurring BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_vacation_requests_user_id ON vacation_requests(user_id);
CREATE INDEX idx_vacation_requests_status ON vacation_requests(status);
CREATE INDEX idx_vacation_requests_dates ON vacation_requests(start_date, end_date);
CREATE INDEX idx_vacation_balances_user_year ON vacation_balances(user_id, year);
CREATE INDEX idx_public_holidays_date ON public_holidays(date);
CREATE INDEX idx_public_holidays_state ON public_holidays(state_code);

-- Seed German federal public holidays (recurring)
INSERT INTO public_holidays (date, name, state_code, is_recurring) VALUES
    ('2024-01-01', 'Neujahr', NULL, TRUE),
    ('2024-04-07', 'Karfreitag', NULL, FALSE),
    ('2024-04-09', 'Ostermontag', NULL, FALSE),
    ('2024-05-01', 'Tag der Arbeit', NULL, TRUE),
    ('2024-05-09', 'Christi Himmelfahrt', NULL, FALSE),
    ('2024-05-20', 'Pfingstmontag', NULL, FALSE),
    ('2024-10-03', 'Tag der Deutschen Einheit', NULL, TRUE),
    ('2024-12-25', 'Erster Weihnachtstag', NULL, TRUE),
    ('2024-12-26', 'Zweiter Weihnachtstag', NULL, TRUE);
