// NOTE: User-facing strings are intentionally hardcoded in German for this implementation phase.
// Full i18n via fluent (.ftl files in terminal/locales/) is planned for a future phase.
// All strings are candidates for extraction — do not add new hardcoded strings.

use chrono::{DateTime, Utc};
use iced::widget::{column, container, row, text, Column, Space};
use iced::{Alignment, Color, Element, Length};
use iced::widget::container::Appearance;

use super::Message;

// ─── Data types ─────────────────────────────────────────────────────────────

/// Represents the different screens displayed on the terminal
#[derive(Debug, Clone, PartialEq)]
pub enum Screen {
    Idle,
    ClockIn(ClockInData),
    ClockOut(ClockOutData),
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

// ─── View helpers ────────────────────────────────────────────────────────────

/// Idle/welcome screen: shows clock and "scan badge" prompt.
pub fn idle_view(
    now: &DateTime<Utc>,
    company_name: &str,
    pending_count: u32,
    is_offline: bool,
) -> Element<'static, Message> {
    let time_str = now.format("%H:%M:%S").to_string();
    let date_str = now.format("%A, %d. %B %Y").to_string();

    let mut col: Column<Message> = column![
        text(company_name.to_string()).size(28),
        Space::with_height(30),
        text(time_str).size(80),
        text(date_str).size(22),
        Space::with_height(50),
        text("Bitte scannen Sie Ihren Ausweis").size(28),
    ]
    .spacing(8)
    .align_items(Alignment::Center);

    if is_offline {
        col = col.push(Space::with_height(20));
        col = col.push(
            text(format!(
                "\u{26A0}  Offline  \u{2014}  {} ausstehende Ereignisse",
                pending_count
            ))
            .size(18)
            .style(Color::from_rgb(1.0, 0.65, 0.0)),
        );
    } else if pending_count > 0 {
        col = col.push(Space::with_height(20));
        col = col.push(
            text(format!(
                "\u{2191}  {} Ereignisse werden synchronisiert",
                pending_count
            ))
            .size(18)
            .style(Color::from_rgb(0.5, 0.8, 1.0)),
        );
    }

    container(col)
        .width(Length::Fill)
        .height(Length::Fill)
        .center_x()
        .center_y()
        .into()
}

/// Loading screen shown while waiting for API response.
pub fn loading_view() -> Element<'static, Message> {
    let col = column![
        text("Verarbeitung\u{2026}").size(36),
        Space::with_height(20),
        text("Bitte warten").size(22),
    ]
    .spacing(8)
    .align_items(Alignment::Center);

    container(col)
        .width(Length::Fill)
        .height(Length::Fill)
        .center_x()
        .center_y()
        .into()
}

/// Green clock-in confirmation screen.
pub fn clock_in_view(data: &ClockInData, seconds_left: u64) -> Element<'static, Message> {
    let col = column![
        text("\u{2713}  Eingestempelt")
            .size(48)
            .style(Color::from_rgb(0.2, 0.9, 0.3)),
        Space::with_height(30),
        text(data.employee_name.clone()).size(36),
        Space::with_height(10),
        text(format!("Uhrzeit: {}", data.timestamp)).size(24),
        Space::with_height(40),
        text(format!("Zur\u{00FC}ck in {}s", seconds_left))
            .size(18)
            .style(Color::from_rgb(0.6, 0.6, 0.6)),
    ]
    .spacing(8)
    .align_items(Alignment::Center);

    container(col)
        .width(Length::Fill)
        .height(Length::Fill)
        .center_x()
        .center_y()
        .style(|_: &iced::Theme| Appearance {
            background: Some(Color::from_rgb(0.05, 0.15, 0.05).into()),
            ..Appearance::default()
        })
        .into()
}

/// Red clock-out confirmation screen with summary.
pub fn clock_out_view(data: &ClockOutData, seconds_left: u64) -> Element<'static, Message> {
    let hours = data.hours_worked as u32;
    let minutes = ((data.hours_worked - hours as f32) * 60.0) as u32;
    let overtime_sign = if data.overtime_minutes >= 0 { "+" } else { "" };
    let overtime_hours = data.overtime_minutes / 60;
    let overtime_mins = data.overtime_minutes.abs() % 60;

    let col = column![
        text("\u{2717}  Ausgestempelt")
            .size(48)
            .style(Color::from_rgb(0.95, 0.2, 0.2)),
        Space::with_height(30),
        text(data.employee_name.clone()).size(36),
        Space::with_height(10),
        text(format!("Uhrzeit: {}", data.timestamp)).size(24),
        Space::with_height(30),
        row![
            summary_item("Arbeitszeit", &format!("{}h {:02}min", hours, minutes)),
            Space::with_width(40),
            summary_item("Pause", &format!("{}min", data.break_minutes)),
        ]
        .align_items(Alignment::Center),
        Space::with_height(10),
        row![
            summary_item(
                "\u{00DC}berstunden",
                &format!("{}{}h {:02}min", overtime_sign, overtime_hours, overtime_mins),
            ),
            Space::with_width(40),
            summary_item(
                "Resturlaub",
                &format!("{:.1} Tage", data.remaining_vacation_days),
            ),
        ]
        .align_items(Alignment::Center),
        Space::with_height(40),
        text(format!("Zur\u{00FC}ck in {}s", seconds_left))
            .size(18)
            .style(Color::from_rgb(0.6, 0.6, 0.6)),
    ]
    .spacing(8)
    .align_items(Alignment::Center);

    container(col)
        .width(Length::Fill)
        .height(Length::Fill)
        .center_x()
        .center_y()
        .style(|_: &iced::Theme| Appearance {
            background: Some(Color::from_rgb(0.15, 0.03, 0.03).into()),
            ..Appearance::default()
        })
        .into()
}

/// Orange offline confirmation screen (event was buffered locally).
pub fn offline_confirm_view(data: &ClockInData, seconds_left: u64) -> Element<'static, Message> {
    let col = column![
        text("\u{2191}  Offline gespeichert")
            .size(42)
            .style(Color::from_rgb(1.0, 0.65, 0.0)),
        Space::with_height(20),
        text("Der Scan wurde lokal gespeichert").size(24),
        text("und beim n\u{00E4}chsten Start synchronisiert.").size(24),
        Space::with_height(20),
        text(format!("Uhrzeit: {}", data.timestamp)).size(20),
        Space::with_height(40),
        text(format!("Zur\u{00FC}ck in {}s", seconds_left))
            .size(18)
            .style(Color::from_rgb(0.6, 0.6, 0.6)),
    ]
    .spacing(8)
    .align_items(Alignment::Center);

    container(col)
        .width(Length::Fill)
        .height(Length::Fill)
        .center_x()
        .center_y()
        .style(|_: &iced::Theme| Appearance {
            background: Some(Color::from_rgb(0.15, 0.10, 0.0).into()),
            ..Appearance::default()
        })
        .into()
}

/// Yellow/orange error screen.
pub fn error_view(data: &ErrorData, seconds_left: u64) -> Element<'static, Message> {
    let (icon, description) = match data.error_type {
        ErrorType::BadgeNotRecognized => (
            "\u{26A0}  Ausweis nicht erkannt",
            "Dieser Ausweis ist nicht registriert.",
        ),
        ErrorType::ServerUnavailable => (
            "\u{26A0}  Server nicht erreichbar",
            "Bitte versuchen Sie es sp\u{00E4}ter erneut.",
        ),
        ErrorType::NetworkError => (
            "\u{26A0}  Netzwerkfehler",
            "Bitte versuchen Sie es sp\u{00E4}ter erneut.",
        ),
        ErrorType::Other => ("\u{26A0}  Fehler", "Ein unbekannter Fehler ist aufgetreten."),
    };

    let col = column![
        text(icon).size(42).style(Color::from_rgb(1.0, 0.65, 0.0)),
        Space::with_height(20),
        text(description).size(24),
        Space::with_height(10),
        text(data.message.clone())
            .size(18)
            .style(Color::from_rgb(0.7, 0.7, 0.7)),
        Space::with_height(40),
        text(format!("Zur\u{00FC}ck in {}s", seconds_left))
            .size(18)
            .style(Color::from_rgb(0.6, 0.6, 0.6)),
    ]
    .spacing(8)
    .align_items(Alignment::Center);

    container(col)
        .width(Length::Fill)
        .height(Length::Fill)
        .center_x()
        .center_y()
        .style(|_: &iced::Theme| Appearance {
            background: Some(Color::from_rgb(0.15, 0.10, 0.0).into()),
            ..Appearance::default()
        })
        .into()
}

// ─── Private helpers ─────────────────────────────────────────────────────────

fn summary_item<'a>(label: &str, value: &str) -> Element<'a, Message> {
    column![
        text(label.to_string())
            .size(16)
            .style(Color::from_rgb(0.7, 0.7, 0.7)),
        text(value.to_string()).size(22),
    ]
    .spacing(4)
    .align_items(Alignment::Center)
    .into()
}
