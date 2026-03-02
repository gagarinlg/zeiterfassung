CREATE TABLE work_hour_change_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    current_weekly_hours DECIMAL(5,2) NOT NULL,
    requested_weekly_hours DECIMAL(5,2) NOT NULL,
    current_daily_hours DECIMAL(5,2),
    requested_daily_hours DECIMAL(5,2),
    effective_date DATE NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID REFERENCES users(id),
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_whcr_user ON work_hour_change_requests(user_id);
CREATE INDEX idx_whcr_status ON work_hour_change_requests(status);
