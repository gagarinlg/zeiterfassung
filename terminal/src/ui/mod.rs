pub mod screens;

use chrono::{DateTime, Utc};
use iced::futures::SinkExt;
use iced::{executor, Application, Command, Element, Settings, Size, Subscription, Theme};
use log::{info, warn};
use std::sync::{Arc, Mutex};
use std::time::Duration;

use crate::api::{ApiClient, ApiError, ClockResponse};
use crate::audio::AudioPlayer;
use crate::buffer::EventBuffer;
use crate::config::AppConfig;
use crate::rfid::RfidReader;
use screens::{ClockInData, ClockOutData, ErrorData, ErrorType};

// ─── Messages ────────────────────────────────────────────────────────────────

#[derive(Debug, Clone)]
pub enum Message {
    /// Clock tick every second — updates the idle clock and state timeouts.
    Tick,
    /// Periodic trigger to attempt syncing buffered offline events.
    SyncTick,
    /// An RFID tag was scanned.
    RfidScanned(String),
    /// API response received after a scan.
    ScanResult(Result<ClockResponse, ApiError>),
    /// Background sync completed; contains the number of events synced.
    SyncComplete(u32),
}

// ─── Application state ───────────────────────────────────────────────────────

enum AppState {
    Idle {
        now: DateTime<Utc>,
    },
    Loading {
        rfid: String,
    },
    ClockIn {
        data: ClockInData,
        seconds_left: u64,
    },
    ClockOut {
        data: ClockOutData,
        seconds_left: u64,
    },
    /// Event was stored offline; shown with amber colour scheme.
    OfflineConfirm {
        data: ClockInData,
        seconds_left: u64,
    },
    Error {
        data: ErrorData,
        seconds_left: u64,
    },
}

struct TerminalApp {
    state: AppState,
    config: AppConfig,
    api_client: ApiClient,
    event_buffer: Arc<Mutex<EventBuffer>>,
    audio: AudioPlayer,
    rfid_reader: Arc<Mutex<RfidReader>>,
    /// Number of buffered events waiting to be synced.
    pending_count: u32,
    /// Whether the last API call succeeded.
    is_online: bool,
    /// Identifier sent with every scan request.
    terminal_id: String,
    /// Debounce tracking: (last_tag, instant it was scanned).
    last_scan_time: Option<(String, std::time::Instant)>,
}

// ─── Application trait ───────────────────────────────────────────────────────

impl Application for TerminalApp {
    type Executor = executor::Default;
    type Message = Message;
    type Theme = Theme;
    type Flags = AppConfig;

    fn new(config: AppConfig) -> (Self, Command<Message>) {
        let api_client = ApiClient::new(&config.api);

        let event_buffer = Arc::new(Mutex::new(
            EventBuffer::new(&config.offline.buffer_path, config.offline.max_buffer_size)
                .unwrap_or_else(|e| {
                    warn!(
                        "Cannot open event buffer at '{}': {}. Falling back to in-memory.",
                        config.offline.buffer_path, e
                    );
                    EventBuffer::new(":memory:", config.offline.max_buffer_size)
                        .expect("in-memory buffer must always succeed")
                }),
        ));

        let pending_count = event_buffer
            .lock()
            .map(|buf| buf.pending_count().unwrap_or(0))
            .unwrap_or(0);

        let audio = AudioPlayer::new(config.audio.clone());

        let rfid_reader = Arc::new(Mutex::new(RfidReader::new(
            &config.rfid.input_device,
            config.rfid.debounce_ms,
        )));

        let terminal_id = config.api.terminal_id.clone();

        let app = TerminalApp {
            state: AppState::Idle { now: Utc::now() },
            config,
            api_client,
            event_buffer,
            audio,
            rfid_reader,
            pending_count,
            is_online: true,
            terminal_id,
            last_scan_time: None,
        };

        info!("Terminal application started");
        (app, Command::none())
    }

    fn title(&self) -> String {
        format!("Zeiterfassung Terminal — {}", self.config.company.name)
    }

    fn theme(&self) -> Theme {
        if self.config.display.theme == "light" {
            Theme::Light
        } else {
            Theme::Dark
        }
    }

    fn update(&mut self, message: Message) -> Command<Message> {
        match message {
            Message::Tick => self.handle_tick(),
            Message::SyncTick => self.handle_sync_tick(),
            Message::RfidScanned(tag_id) => self.handle_rfid_scanned(tag_id),
            Message::ScanResult(result) => self.handle_scan_result(result),
            Message::SyncComplete(count) => self.handle_sync_complete(count),
        }
    }

    fn view(&self) -> Element<'_, Message, Theme, iced::Renderer> {
        match &self.state {
            AppState::Idle { now } => screens::idle_view(
                now,
                &self.config.company.name,
                self.pending_count,
                !self.is_online,
            ),
            AppState::Loading { .. } => screens::loading_view(),
            AppState::ClockIn { data, seconds_left } => screens::clock_in_view(data, *seconds_left),
            AppState::ClockOut { data, seconds_left } => {
                screens::clock_out_view(data, *seconds_left)
            }
            AppState::OfflineConfirm { data, seconds_left } => {
                screens::offline_confirm_view(data, *seconds_left)
            }
            AppState::Error { data, seconds_left } => screens::error_view(data, *seconds_left),
        }
    }

    fn subscription(&self) -> Subscription<Message> {
        let tick = iced::time::every(Duration::from_secs(1)).map(|_| Message::Tick);

        let sync_interval = Duration::from_secs(self.config.offline.sync_interval_seconds.max(5));
        let sync_tick = iced::time::every(sync_interval).map(|_| Message::SyncTick);

        let rfid = rfid_subscription(Arc::clone(&self.rfid_reader));

        Subscription::batch(vec![tick, sync_tick, rfid])
    }
}

// ─── Update helpers ───────────────────────────────────────────────────────────

impl TerminalApp {
    fn handle_tick(&mut self) -> Command<Message> {
        let return_to_idle = match &mut self.state {
            AppState::Idle { now } => {
                *now = Utc::now();
                false
            }
            AppState::ClockIn { seconds_left, .. }
            | AppState::ClockOut { seconds_left, .. }
            | AppState::OfflineConfirm { seconds_left, .. }
            | AppState::Error { seconds_left, .. } => {
                if *seconds_left > 0 {
                    *seconds_left -= 1;
                    false
                } else {
                    true
                }
            }
            AppState::Loading { .. } => false,
        };

        if return_to_idle {
            self.state = AppState::Idle { now: Utc::now() };
        }
        Command::none()
    }

    fn handle_sync_tick(&mut self) -> Command<Message> {
        if self.pending_count == 0 {
            return Command::none();
        }

        let buffer = Arc::clone(&self.event_buffer);
        let api = self.api_client.clone();

        Command::perform(
            async move { sync_buffered_events(api, buffer).await },
            Message::SyncComplete,
        )
    }

    fn handle_rfid_scanned(&mut self, tag_id: String) -> Command<Message> {
        // Only process scans while idle.
        if !matches!(self.state, AppState::Idle { .. }) {
            return Command::none();
        }

        // Debounce: ignore the same tag scanned within the configured window.
        let now = std::time::Instant::now();
        if let Some((ref last_id, last_time)) = self.last_scan_time {
            if last_id == &tag_id
                && now.duration_since(last_time)
                    < Duration::from_millis(self.config.rfid.debounce_ms)
            {
                return Command::none();
            }
        }
        self.last_scan_time = Some((tag_id.clone(), now));

        info!("RFID scanned: {}", tag_id);
        self.state = AppState::Loading {
            rfid: tag_id.clone(),
        };

        let api = self.api_client.clone();
        let rfid = tag_id.clone();
        let terminal_id = self.terminal_id.clone();

        Command::perform(
            async move { api.clock_in_out(&rfid, &terminal_id).await },
            Message::ScanResult,
        )
    }

    fn handle_scan_result(&mut self, result: Result<ClockResponse, ApiError>) -> Command<Message> {
        // Extract the RFID from the loading state; ignore results that arrive late.
        let rfid = match &self.state {
            AppState::Loading { rfid } => rfid.clone(),
            _ => return Command::none(),
        };

        match result {
            Ok(response) => {
                self.is_online = true;
                self.audio.play_success();
                // Refresh pending count in case a previous sync cleared some events.
                if let Ok(buf) = self.event_buffer.lock() {
                    self.pending_count = buf.pending_count().unwrap_or(0);
                }

                let name = format!(
                    "{} {}",
                    response.employee.first_name, response.employee.last_name
                );
                let ts = response.timestamp.format("%H:%M:%S").to_string();
                let timeout = self.config.display.idle_timeout_seconds;

                if response.entry_type == "CLOCK_IN" {
                    self.state = AppState::ClockIn {
                        data: ClockInData {
                            employee_name: name,
                            timestamp: ts,
                            scheduled_hours: 8.0,
                        },
                        seconds_left: timeout,
                    };
                } else {
                    self.state = AppState::ClockOut {
                        data: ClockOutData {
                            employee_name: name,
                            timestamp: ts,
                            hours_worked: response.today_work_minutes as f32 / 60.0,
                            break_minutes: response.today_break_minutes,
                            weekly_hours_worked: 0.0,
                            weekly_hours_target: 40.0,
                            overtime_minutes: response.overtime_minutes,
                            remaining_vacation_days: response.remaining_vacation_days,
                        },
                        seconds_left: timeout,
                    };
                }
            }

            Err(ApiError::NetworkError(_)) | Err(ApiError::Timeout) => {
                // Store event locally and show optimistic offline confirmation.
                if let Ok(buf) = self.event_buffer.lock() {
                    let _ = buf.push(&rfid, &self.terminal_id);
                    self.pending_count = buf.pending_count().unwrap_or(0);
                }
                self.is_online = false;
                self.audio.play_success(); // Optimistic feedback.

                self.state = AppState::OfflineConfirm {
                    data: ClockInData {
                        employee_name: String::new(),
                        timestamp: Utc::now().format("%H:%M:%S").to_string(),
                        scheduled_hours: 0.0,
                    },
                    seconds_left: self.config.display.idle_timeout_seconds,
                };
            }

            Err(ApiError::NotFound(_)) => {
                self.audio.play_error();
                self.state = AppState::Error {
                    data: ErrorData {
                        message: "Ausweis nicht registriert".to_string(),
                        error_type: ErrorType::BadgeNotRecognized,
                    },
                    seconds_left: self.config.display.error_timeout_seconds,
                };
            }

            // 409 — another terminal toggled this employee's status at the same instant.
            // Tell the user to scan once more; the next scan will succeed.
            Err(ApiError::Conflict) => {
                self.audio.play_error();
                warn!("Scan conflict for RFID {}: another terminal processed the same badge simultaneously", rfid);
                self.state = AppState::Error {
                    data: ErrorData {
                        message: "Bitte erneut scannen".to_string(),
                        error_type: ErrorType::Other,
                    },
                    seconds_left: self.config.display.error_timeout_seconds,
                };
            }

            Err(err) => {
                self.audio.play_error();
                warn!("Scan error: {}", err);
                self.state = AppState::Error {
                    data: ErrorData {
                        message: err.to_string(),
                        error_type: ErrorType::ServerUnavailable,
                    },
                    seconds_left: self.config.display.error_timeout_seconds,
                };
            }
        }

        Command::none()
    }

    fn handle_sync_complete(&mut self, count: u32) -> Command<Message> {
        if count > 0 {
            info!("Synced {} buffered events", count);
            if let Ok(buf) = self.event_buffer.lock() {
                self.pending_count = buf.pending_count().unwrap_or(0);
            }
            if self.pending_count == 0 {
                self.is_online = true;
            }
        }
        Command::none()
    }
}

// ─── RFID subscription ────────────────────────────────────────────────────────

/// Creates an iced subscription that polls the RfidReader at 50 ms intervals
/// and forwards any scanned tags as `Message::RfidScanned`.
fn rfid_subscription(reader: Arc<Mutex<RfidReader>>) -> Subscription<Message> {
    use std::any::TypeId;

    struct RfidId;

    iced::subscription::channel(TypeId::of::<RfidId>(), 10, move |mut sender| async move {
        loop {
            tokio::time::sleep(Duration::from_millis(50)).await;

            let tag = reader.lock().ok().and_then(|mut r| r.poll());

            if let Some(tag_id) = tag {
                let _ = sender.send(Message::RfidScanned(tag_id)).await;
            }
        }
    })
}

// ─── Background sync ─────────────────────────────────────────────────────────

/// Attempts to sync all pending buffered events with the API.
/// Returns the number of events successfully synced.
///
/// Offline events are replayed in FIFO order.  Three outcomes are possible for each event:
///
/// * **Success** — the server accepted it; mark synced.
/// * **Network/timeout** — backend still unreachable; stop and retry on the next tick.
/// * **409 Conflict** — another terminal already changed this employee's status while this
///   terminal was offline.  The event is stale; discard it so it is never retried.
/// * **404 Not found** — the RFID tag was deregistered while offline; discard.
/// * **Other server error** — discard to avoid blocking the queue indefinitely.
async fn sync_buffered_events(api: ApiClient, buffer: Arc<Mutex<EventBuffer>>) -> u32 {
    let events = match buffer.lock() {
        Ok(buf) => buf.get_pending().unwrap_or_default(),
        Err(_) => return 0,
    };

    let mut synced = 0u32;
    for event in &events {
        if let Some(id) = event.id {
            match api
                .clock_in_out(&event.rfid_tag_id, &event.terminal_id)
                .await
            {
                Ok(_) => {
                    if let Ok(buf) = buffer.lock() {
                        let _ = buf.mark_synced(id);
                    }
                    synced += 1;
                }
                // Backend unreachable — stop and retry later.
                Err(ApiError::NetworkError(_)) | Err(ApiError::Timeout) => break,
                // Stale or invalid events — discard to keep the queue moving.
                Err(ApiError::Conflict) | Err(ApiError::NotFound(_)) | Err(_) => {
                    warn!(
                        "Discarding buffered event id={} rfid={}: already processed or invalid",
                        id, event.rfid_tag_id
                    );
                    if let Ok(buf) = buffer.lock() {
                        let _ = buf.mark_synced(id);
                    }
                }
            }
        }
    }
    synced
}

// ─── Entry point ─────────────────────────────────────────────────────────────

pub fn run(config: AppConfig) -> Result<(), Box<dyn std::error::Error>> {
    let width = config.display.resolution_width() as f32;
    let height = config.display.resolution_height() as f32;
    let decorations = !config.display.fullscreen;

    let window = iced::window::Settings {
        size: Size::new(width, height),
        decorations,
        ..Default::default()
    };

    let settings = Settings {
        window,
        flags: config,
        ..Default::default()
    };

    TerminalApp::run(settings).map_err(|e| Box::new(e) as Box<dyn std::error::Error>)
}
