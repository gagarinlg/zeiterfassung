-- V2: Time Tracking Tables

CREATE TABLE time_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN ('CLOCK_IN', 'CLOCK_OUT', 'BREAK_START', 'BREAK_END')),
    timestamp TIMESTAMPTZ NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'WEB' CHECK (source IN ('WEB', 'MOBILE', 'TERMINAL')),
    terminal_id VARCHAR(100),
    notes TEXT,
    is_modified BOOLEAN NOT NULL DEFAULT FALSE,
    modified_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE daily_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_work_minutes INT NOT NULL DEFAULT 0,
    total_break_minutes INT NOT NULL DEFAULT 0,
    overtime_minutes INT NOT NULL DEFAULT 0,
    is_compliant BOOLEAN NOT NULL DEFAULT TRUE,
    compliance_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, date)
);

-- Indexes
CREATE INDEX idx_time_entries_user_id ON time_entries(user_id);
CREATE INDEX idx_time_entries_timestamp ON time_entries(timestamp);
CREATE INDEX idx_time_entries_user_timestamp ON time_entries(user_id, timestamp);
CREATE INDEX idx_daily_summaries_user_id ON daily_summaries(user_id);
CREATE INDEX idx_daily_summaries_date ON daily_summaries(date);
CREATE INDEX idx_daily_summaries_user_date ON daily_summaries(user_id, date);
