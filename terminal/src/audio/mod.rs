use log::{debug, warn};
use rodio::{Decoder, OutputStream, Sink};
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

        std::thread::spawn(move || {
            match OutputStream::try_default() {
                Ok((_stream, stream_handle)) => match Sink::try_new(&stream_handle) {
                    Ok(sink) => {
                        sink.set_volume(volume);
                        match File::open(&path) {
                            Ok(file) => {
                                let reader = BufReader::new(file);
                                match Decoder::new(reader) {
                                    Ok(source) => {
                                        sink.append(source);
                                        sink.sleep_until_end();
                                        debug!("Played sound: {}", path);
                                    }
                                    Err(e) => warn!("Failed to decode audio {}: {}", path, e),
                                }
                            }
                            Err(e) => warn!("Failed to open audio file {}: {}", path, e),
                        }
                    }
                    Err(e) => warn!("Failed to create audio sink: {}", e),
                },
                Err(e) => warn!("Failed to initialize audio output: {}", e),
            }
        });
    }
}
