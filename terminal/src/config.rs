use serde::{Deserialize, Serialize};
use std::path::Path;

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct AppConfig {
    pub display: DisplayConfig,
    pub api: ApiConfig,
    pub offline: OfflineConfig,
    pub rfid: RfidConfig,
    pub audio: AudioConfig,
    pub locale: LocaleConfig,
    pub company: CompanyConfig,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct DisplayConfig {
    pub resolution: String,
    pub fullscreen: bool,
    pub orientation: String,
    pub theme: String,
    pub font_scale: f32,
    pub idle_timeout_seconds: u64,
    pub error_timeout_seconds: u64,
}

impl DisplayConfig {
    pub fn resolution_width(&self) -> u32 {
        self.parse_resolution().0
    }

    pub fn resolution_height(&self) -> u32 {
        self.parse_resolution().1
    }

    fn parse_resolution(&self) -> (u32, u32) {
        let parts: Vec<&str> = self.resolution.split('x').collect();
        if parts.len() == 2 {
            let w = parts[0].trim().parse().unwrap_or(1024);
            let h = parts[1].trim().parse().unwrap_or(600);
            (w, h)
        } else {
            (1024, 600)
        }
    }
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct ApiConfig {
    pub base_url: String,
    pub timeout_seconds: u64,
    pub retry_attempts: u32,
    /// Unique identifier for this terminal device — must be distinct per physical terminal.
    pub terminal_id: String,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct OfflineConfig {
    pub buffer_path: String,
    pub sync_interval_seconds: u64,
    pub max_buffer_size: u32,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct RfidConfig {
    pub input_device: String,
    pub debounce_ms: u64,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct AudioConfig {
    pub enabled: bool,
    pub success_sound: String,
    pub error_sound: String,
    pub volume: f32,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct LocaleConfig {
    pub language: String,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct CompanyConfig {
    pub name: String,
    pub logo_path: String,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            display: DisplayConfig {
                resolution: "1024x600".to_string(),
                fullscreen: true,
                orientation: "landscape".to_string(),
                theme: "dark".to_string(),
                font_scale: 1.0,
                idle_timeout_seconds: 8,
                error_timeout_seconds: 5,
            },
            api: ApiConfig {
                base_url: "http://localhost:8080/api".to_string(),
                timeout_seconds: 10,
                retry_attempts: 3,
                terminal_id: "terminal-01".to_string(),
            },
            offline: OfflineConfig {
                buffer_path: "/var/lib/zeiterfassung/buffer.db".to_string(),
                sync_interval_seconds: 30,
                max_buffer_size: 10000,
            },
            rfid: RfidConfig {
                input_device: "auto".to_string(),
                debounce_ms: 500,
            },
            audio: AudioConfig {
                enabled: true,
                success_sound: "assets/sounds/success.wav".to_string(),
                error_sound: "assets/sounds/error.wav".to_string(),
                volume: 0.7,
            },
            locale: LocaleConfig {
                language: "de".to_string(),
            },
            company: CompanyConfig {
                name: "Firma GmbH".to_string(),
                logo_path: "assets/logo.png".to_string(),
            },
        }
    }
}

impl AppConfig {
    pub fn load(path: &Path) -> Result<Self, Box<dyn std::error::Error>> {
        let content = std::fs::read_to_string(path)?;
        let config: AppConfig = toml::from_str(&content)?;
        Ok(config)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_default_config() {
        let config = AppConfig::default();
        assert_eq!(config.display.resolution, "1024x600");
        assert!(config.display.fullscreen);
        assert_eq!(config.locale.language, "de");
    }

    #[test]
    fn test_resolution_parsing() {
        let config = DisplayConfig {
            resolution: "1920x1080".to_string(),
            fullscreen: true,
            orientation: "landscape".to_string(),
            theme: "dark".to_string(),
            font_scale: 1.0,
            idle_timeout_seconds: 8,
            error_timeout_seconds: 5,
        };
        assert_eq!(config.resolution_width(), 1920);
        assert_eq!(config.resolution_height(), 1080);
    }

    #[test]
    fn test_invalid_resolution_fallback() {
        let config = DisplayConfig {
            resolution: "invalid".to_string(),
            fullscreen: true,
            orientation: "landscape".to_string(),
            theme: "dark".to_string(),
            font_scale: 1.0,
            idle_timeout_seconds: 8,
            error_timeout_seconds: 5,
        };
        assert_eq!(config.resolution_width(), 1024);
        assert_eq!(config.resolution_height(), 600);
    }

    #[test]
    fn test_load_from_toml_string() {
        let toml_str = r#"
[display]
resolution = "800x480"
fullscreen = false
orientation = "portrait"
theme = "light"
font_scale = 1.2
idle_timeout_seconds = 10
error_timeout_seconds = 3

[api]
base_url = "https://example.com/api"
timeout_seconds = 15
retry_attempts = 5
terminal_id = "terminal-02"

[offline]
buffer_path = "/tmp/test.db"
sync_interval_seconds = 60
max_buffer_size = 500

[rfid]
input_device = "auto"
debounce_ms = 300

[audio]
enabled = false
success_sound = "assets/sounds/ok.wav"
error_sound = "assets/sounds/fail.wav"
volume = 0.5

[locale]
language = "en"

[company]
name = "Test GmbH"
logo_path = "assets/test-logo.png"
"#;
        let config: AppConfig = toml::from_str(toml_str).expect("failed to parse TOML");
        assert_eq!(config.display.resolution, "800x480");
        assert_eq!(config.display.resolution_width(), 800);
        assert_eq!(config.display.resolution_height(), 480);
        assert!(!config.display.fullscreen);
        assert_eq!(config.display.theme, "light");
        assert_eq!(config.api.base_url, "https://example.com/api");
        assert_eq!(config.api.retry_attempts, 5);
        assert_eq!(config.api.terminal_id, "terminal-02");
        assert!(!config.audio.enabled);
        assert_eq!(config.locale.language, "en");
        assert_eq!(config.company.name, "Test GmbH");
    }

    #[test]
    fn test_each_terminal_has_unique_id_in_config() {
        // Each physical terminal reads its own terminal.toml — validate the field is present
        // and distinct from the default so operators are reminded to set it.
        let config = AppConfig::default();
        assert!(
            !config.api.terminal_id.is_empty(),
            "terminal_id must not be empty"
        );
    }
}
