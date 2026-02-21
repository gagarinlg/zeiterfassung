use chrono::{DateTime, Utc};
use log::{error, warn};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::time::Duration;

use crate::config::ApiConfig;

#[derive(Debug, Clone, Deserialize)]
pub struct EmployeeInfo {
    pub id: String,
    pub first_name: String,
    pub last_name: String,
    pub photo_url: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ClockResponse {
    pub employee: EmployeeInfo,
    pub entry_type: String,
    pub timestamp: DateTime<Utc>,
    pub today_work_minutes: u32,
    pub today_break_minutes: u32,
    pub overtime_minutes: i32,
    pub remaining_vacation_days: f32,
}

#[derive(Debug, Serialize)]
struct ClockRequest {
    rfid_tag_id: String,
    terminal_id: String,
}

#[derive(Debug, Clone)]
pub struct ApiClient {
    client: Client,
    base_url: String,
    retry_attempts: u32,
}

#[derive(Debug)]
pub enum ApiError {
    NotFound(String),
    Unauthorized,
    ServerError(String),
    NetworkError(String),
    Timeout,
}

impl std::fmt::Display for ApiError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ApiError::NotFound(msg) => write!(f, "Not found: {}", msg),
            ApiError::Unauthorized => write!(f, "Unauthorized"),
            ApiError::ServerError(msg) => write!(f, "Server error: {}", msg),
            ApiError::NetworkError(msg) => write!(f, "Network error: {}", msg),
            ApiError::Timeout => write!(f, "Request timed out"),
        }
    }
}

impl ApiClient {
    pub fn new(config: &ApiConfig) -> Self {
        let client = Client::builder()
            .timeout(Duration::from_secs(config.timeout_seconds))
            .build()
            .expect("Failed to create HTTP client");

        Self {
            client,
            base_url: config.base_url.clone(),
            retry_attempts: config.retry_attempts,
        }
    }

    pub async fn clock_in_out(&self, rfid_tag_id: &str, terminal_id: &str) -> Result<ClockResponse, ApiError> {
        let url = format!("{}/terminal/clock", self.base_url);
        let request = ClockRequest {
            rfid_tag_id: rfid_tag_id.to_string(),
            terminal_id: terminal_id.to_string(),
        };

        let mut last_error = ApiError::NetworkError("No attempts made".to_string());

        for attempt in 1..=self.retry_attempts {
            match self.client.post(&url).json(&request).send().await {
                Ok(response) => {
                    let status = response.status();
                    if status.is_success() {
                        return response.json::<ClockResponse>().await.map_err(|e| {
                            ApiError::ServerError(format!("Failed to parse response: {}", e))
                        });
                    } else if status.as_u16() == 404 {
                        return Err(ApiError::NotFound("RFID tag not registered".to_string()));
                    } else if status.as_u16() == 401 {
                        return Err(ApiError::Unauthorized);
                    } else {
                        last_error = ApiError::ServerError(format!("HTTP {}", status));
                    }
                }
                Err(e) if e.is_timeout() => {
                    warn!("Request timed out (attempt {}/{})", attempt, self.retry_attempts);
                    last_error = ApiError::Timeout;
                }
                Err(e) => {
                    warn!("Network error (attempt {}/{}): {}", attempt, self.retry_attempts, e);
                    last_error = ApiError::NetworkError(e.to_string());
                }
            }
        }

        error!("All {} retry attempts failed", self.retry_attempts);
        Err(last_error)
    }

    pub async fn health_check(&self) -> bool {
        let url = format!("{}/actuator/health", self.base_url);
        match self.client.get(&url).send().await {
            Ok(response) => response.status().is_success(),
            Err(_) => false,
        }
    }
}
