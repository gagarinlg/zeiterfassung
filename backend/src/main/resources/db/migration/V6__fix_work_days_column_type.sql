-- V6: Fix work_days column type in employee_configs
-- The JPA entity uses columnDefinition = "text" for H2 compatibility,
-- so the PostgreSQL column must also be text (not jsonb).
ALTER TABLE employee_configs
    ALTER COLUMN work_days TYPE TEXT USING work_days::text;
