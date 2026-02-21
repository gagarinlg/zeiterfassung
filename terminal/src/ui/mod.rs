pub mod screens;

use crate::config::AppConfig;

pub fn run(config: AppConfig) -> Result<(), Box<dyn std::error::Error>> {
    log::info!("UI subsystem initializing...");
    log::info!(
        "Resolution: {}x{}, Theme: {}, Fullscreen: {}",
        config.display.resolution_width(),
        config.display.resolution_height(),
        config.display.theme,
        config.display.fullscreen
    );
    // TODO: Initialize iced application with config
    // iced::application("Zeiterfassung Terminal", App::update, App::view)
    //     .window_size((config.display.resolution_width() as f32, config.display.resolution_height() as f32))
    //     .run()
    log::info!("UI initialized (placeholder - full iced integration pending)");
    Ok(())
}
