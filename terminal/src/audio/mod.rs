use log::{debug, warn};
use rodio::{Decoder, Player};
use std::fs::File;
use std::io::BufReader;

use crate::config::AudioConfig;

pub struct AudioPlayer {
    config: AudioConfig,
}

impl AudioPlayer {
    pub fn new(config: AudioConfig) -> Self {
        Self { config }
    }

    pub fn play_success(&self) {
        if self.config.enabled {
            self.play_sound(&self.config.success_sound);
        }
    }

    pub fn play_error(&self) {
        if self.config.enabled {
            self.play_sound(&self.config.error_sound);
        }
    }

    fn play_sound(&self, path: &str) {
        let path = path.to_string();
        let volume = self.config.volume;

        std::thread::spawn(
            move || match rodio::DeviceSinkBuilder::open_default_sink() {
                Ok(handle) => {
                    let player = Player::connect_new(handle.mixer());
                    player.set_volume(volume);
                    match File::open(&path) {
                        Ok(file) => {
                            let reader = BufReader::new(file);
                            match Decoder::new(reader) {
                                Ok(source) => {
                                    player.append(source);
                                    player.sleep_until_end();
                                    debug!("Played sound: {}", path);
                                }
                                Err(e) => warn!("Failed to decode audio {}: {}", path, e),
                            }
                        }
                        Err(e) => warn!("Failed to open audio file {}: {}", path, e),
                    }
                }
                Err(e) => warn!("Failed to initialize audio output: {}", e),
            },
        );
    }
}
