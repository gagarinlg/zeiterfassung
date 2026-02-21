use log::{debug, info, warn};
use std::sync::mpsc;
use std::thread;
use std::time::{Duration, Instant};

pub struct RfidReader {
    receiver: mpsc::Receiver<String>,
    debounce_ms: u64,
    last_scan: Option<(String, Instant)>,
}

impl RfidReader {
    pub fn new(input_device: &str, debounce_ms: u64) -> Self {
        let (sender, receiver) = mpsc::channel();
        let device = input_device.to_string();

        thread::spawn(move || {
            info!("RFID reader started, input device: {}", device);
            Self::read_stdin(sender);
        });

        Self {
            receiver,
            debounce_ms,
            last_scan: None,
        }
    }

    fn read_stdin(sender: mpsc::Sender<String>) {
        use std::io::BufRead;
        let stdin = std::io::stdin();
        for line in stdin.lock().lines() {
            match line {
                Ok(tag_id) => {
                    let tag_id = tag_id.trim().to_string();
                    if !tag_id.is_empty() {
                        debug!("RFID tag scanned: {}", tag_id);
                        if sender.send(tag_id).is_err() {
                            warn!("RFID receiver dropped, stopping reader");
                            break;
                        }
                    }
                }
                Err(e) => {
                    warn!("Error reading RFID input: {}", e);
                    break;
                }
            }
        }
    }

    pub fn poll(&mut self) -> Option<String> {
        while let Ok(tag_id) = self.receiver.try_recv() {
            let now = Instant::now();

            let is_duplicate = self
                .last_scan
                .as_ref()
                .map(|(last_id, last_time)| {
                    last_id == &tag_id
                        && last_time.elapsed() < Duration::from_millis(self.debounce_ms)
                })
                .unwrap_or(false);

            if !is_duplicate {
                self.last_scan = Some((tag_id.clone(), now));
                return Some(tag_id);
            }
        }
        None
    }
}
