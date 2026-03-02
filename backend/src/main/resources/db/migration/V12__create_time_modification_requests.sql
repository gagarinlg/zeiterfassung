CREATE TABLE time_modification_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    time_entry_id UUID NOT NULL REFERENCES time_entries(id),
    requested_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    requested_notes TEXT,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users(id),
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tmr_user ON time_modification_requests(user_id);
CREATE INDEX idx_tmr_status ON time_modification_requests(status);
CREATE INDEX idx_tmr_time_entry ON time_modification_requests(time_entry_id);
