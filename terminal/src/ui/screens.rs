/// Represents the different screens displayed on the terminal
#[derive(Debug, Clone, PartialEq)]
pub enum Screen {
    /// Default screen shown when idle - shows clock and "scan badge" prompt
    Idle,
    /// Shown after successful clock-in - green color scheme
    ClockIn(ClockInData),
    /// Shown after successful clock-out - red color scheme
    ClockOut(ClockOutData),
    /// Shown for errors - yellow/orange color scheme
    Error(ErrorData),
}

#[derive(Debug, Clone, PartialEq)]
pub struct ClockInData {
    pub employee_name: String,
    pub timestamp: String,
    pub scheduled_hours: f32,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ClockOutData {
    pub employee_name: String,
    pub timestamp: String,
    pub hours_worked: f32,
    pub break_minutes: u32,
    pub weekly_hours_worked: f32,
    pub weekly_hours_target: f32,
    pub overtime_minutes: i32,
    pub remaining_vacation_days: f32,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ErrorData {
    pub message: String,
    pub error_type: ErrorType,
}

#[derive(Debug, Clone, PartialEq)]
pub enum ErrorType {
    BadgeNotRecognized,
    ServerUnavailable,
    NetworkError,
    Other,
}
