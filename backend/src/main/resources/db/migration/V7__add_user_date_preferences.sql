-- Add date/time format preferences to users table
ALTER TABLE users ADD COLUMN date_format VARCHAR(20);
ALTER TABLE users ADD COLUMN time_format VARCHAR(10);

-- Insert system-wide default date/time format settings
INSERT INTO system_settings (key, value, description) VALUES
  ('display.date_format', 'DD.MM.YYYY', 'Default date format (DD.MM.YYYY, YYYY-MM-DD, MM/DD/YYYY)'),
  ('display.time_format', '24h', 'Default time format (24h, 12h)'),
  ('display.first_day_of_week', '1', 'First day of week (1=Monday, 0=Sunday)')
ON CONFLICT (key) DO NOTHING;
