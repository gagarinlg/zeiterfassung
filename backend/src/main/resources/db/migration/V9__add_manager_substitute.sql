-- V9: Add substitute_id column to users table for manager deputy feature
-- A substitute can act on behalf of a manager for their subordinates

ALTER TABLE users ADD COLUMN substitute_id UUID;
ALTER TABLE users ADD CONSTRAINT fk_users_substitute FOREIGN KEY (substitute_id) REFERENCES users(id);
CREATE INDEX idx_users_substitute_id ON users(substitute_id);
