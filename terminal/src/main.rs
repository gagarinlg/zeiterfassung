mod api;
mod audio;
mod buffer;
mod config;
mod rfid;
mod ui;

use std::path::PathBuf;

use config::AppConfig;
use log::{error, info};

fn main() {
    env_logger::init();

    let config_path = std::env::var("CONFIG_PATH")
        .map(PathBuf::from)
        .unwrap_or_else(|_| PathBuf::from("terminal.toml"));

    let config = match AppConfig::load(&config_path) {
        Ok(c) => {
            info!("Configuration loaded from {}", config_path.display());
            c
        }
        Err(e) => {
            error!("Failed to load configuration: {}. Using defaults.", e);
            AppConfig::default()
        }
    };

    info!(
        "Starting Zeiterfassung Terminal v{}",
        env!("CARGO_PKG_VERSION")
    );
    info!("Display: {}x{}", config.display.resolution_width(), config.display.resolution_height());
    info!("API endpoint: {}", config.api.base_url);
    info!("Language: {}", config.locale.language);

    if let Err(e) = ui::run(config) {
        error!("Fatal error in UI: {}", e);
        std::process::exit(1);
    }
}
