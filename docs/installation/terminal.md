# Raspberry Pi Terminal Installation Guide

This guide covers the complete installation and configuration of the Zeiterfassung terminal application on a Raspberry Pi 5 with an attached display and NFC/RFID reader.

---

## Table of Contents

- [Hardware Requirements](#hardware-requirements)
- [OS Setup](#os-setup)
- [System Dependencies](#system-dependencies)
- [Installation](#installation)
  - [Using a Pre-built Binary](#using-a-pre-built-binary)
  - [Building from Source](#building-from-source)
  - [Cross-Compilation from x86\_64](#cross-compilation)
- [Configuration](#configuration)
  - [terminal.toml Reference](#terminaltoml-reference)
- [Display Setup](#display-setup)
  - [Screen Resolution](#screen-resolution)
  - [Kiosk Mode](#kiosk-mode)
  - [Auto-Start on Boot](#auto-start-on-boot)
- [RFID Reader Setup](#rfid-reader-setup)
- [Audio Configuration](#audio-configuration)
- [Systemd Service](#systemd-service)
- [Network Configuration](#network-configuration)
- [Offline Mode & Data Buffering](#offline-mode--data-buffering)
- [Maintenance & Updates](#maintenance--updates)
- [Troubleshooting](#troubleshooting)

---

## Hardware Requirements

| Component | Requirement | Notes |
|-----------|-------------|-------|
| **Board** | Raspberry Pi 5 (4 GB+ RAM) | Raspberry Pi 4 also works but is slower |
| **Storage** | 16 GB+ microSD (Class 10 / A2) or USB SSD | SSD recommended for reliability |
| **Display** | Any HDMI or DSI display | Common: 7" 1024×600, official 7" DSI touchscreen |
| **NFC/RFID Reader** | USB HID keyboard-emulating reader | Any reader that sends tag ID + newline as keyboard input |
| **Speaker** | USB or 3.5mm audio output (optional) | For audible clock-in/clock-out feedback |
| **Power Supply** | Official Raspberry Pi 5 USB-C 27W PSU | Adequate power is critical for stability |
| **Network** | Ethernet (recommended) or Wi-Fi | Ethernet preferred for reliability |
| **Case** | Optional | Consider a case with display mount for kiosk deployments |

### Tested RFID Readers

Any USB RFID/NFC reader that emulates a keyboard (HID mode) will work. The reader sends the tag ID as keystrokes followed by Enter (newline). Common compatible readers include:

- ACR122U (NFC, with HID keyboard firmware)
- Neuftech USB RFID Reader (125 kHz)
- Generic EM4100/EM4200 USB readers

---

## OS Setup

### Install Raspberry Pi OS Lite

1. Download [Raspberry Pi Imager](https://www.raspberrypi.com/software/).
2. Select **Raspberry Pi OS Lite (64-bit)** — the desktop environment is not needed.
3. Configure Wi-Fi, SSH, hostname, and user credentials in the imager settings.
4. Flash the image to your microSD card or SSD.

### Initial System Setup

```bash
# Update the system
sudo apt update && sudo apt full-upgrade -y

# Set timezone (important for correct timestamps)
sudo timedatectl set-timezone Europe/Berlin

# Set locale
sudo localectl set-locale LANG=de_DE.UTF-8

# Update firmware
sudo rpi-update
sudo reboot
```

---

## System Dependencies

Install the required system libraries:

```bash
sudo apt install -y \
  libasound2-dev \
  libssl-dev \
  libsqlite3-dev \
  libwayland-dev \
  libxkbcommon-dev \
  libvulkan-dev \
  pkg-config \
  build-essential \
  fonts-noto \
  cage
```

| Package | Purpose |
|---------|---------|
| `libasound2-dev` | Audio playback (ALSA backend for rodio) |
| `libssl-dev` | TLS for HTTPS API communication |
| `libsqlite3-dev` | Offline event buffer storage |
| `libwayland-dev` | Wayland display protocol (for iced GUI) |
| `libxkbcommon-dev` | Keyboard handling (for RFID input) |
| `libvulkan-dev` | GPU-accelerated rendering (optional, falls back to software) |
| `pkg-config` | Build-time library discovery |
| `fonts-noto` | Unicode font coverage for the GUI |
| `cage` | Minimal Wayland compositor for kiosk mode |

---

## Installation

### Using a Pre-built Binary

If a pre-built binary is available for `aarch64-unknown-linux-gnu`:

```bash
# Download the binary (example URL — use actual release URL)
wget https://github.com/yourorg/zeiterfassung/releases/latest/download/zeiterfassung-terminal-aarch64

# Make executable and move to system path
chmod +x zeiterfassung-terminal-aarch64
sudo mv zeiterfassung-terminal-aarch64 /usr/local/bin/zeiterfassung-terminal

# Create configuration and data directories
sudo mkdir -p /etc/zeiterfassung
sudo mkdir -p /var/lib/zeiterfassung

# Copy configuration
sudo cp terminal.toml /etc/zeiterfassung/terminal.toml

# Copy assets
sudo cp -r assets/ /opt/zeiterfassung/assets/
sudo cp -r locales/ /opt/zeiterfassung/locales/
```

### Building from Source

#### On the Raspberry Pi

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source "$HOME/.cargo/env"

# Clone the repository
git clone https://github.com/yourorg/zeiterfassung.git
cd zeiterfassung/terminal

# Build in release mode
cargo build --release
```

The binary is output to `target/release/zeiterfassung-terminal`.

> **Note:** Building on the Raspberry Pi 5 takes approximately 10–15 minutes for a clean release build.

#### Install After Building

```bash
sudo cp target/release/zeiterfassung-terminal /usr/local/bin/
sudo mkdir -p /etc/zeiterfassung /var/lib/zeiterfassung /opt/zeiterfassung
sudo cp terminal.toml /etc/zeiterfassung/terminal.toml
sudo cp -r assets/ /opt/zeiterfassung/assets/
sudo cp -r locales/ /opt/zeiterfassung/locales/
```

<a id="cross-compilation"></a>

### Cross-Compilation from x86_64

Cross-compiling on a fast development machine is significantly faster than building on the Pi.

#### Setup Cross-Compilation Toolchain

```bash
# Install the aarch64 target
rustup target add aarch64-unknown-linux-gnu

# Install the cross-compilation linker (Ubuntu/Debian)
sudo apt install -y gcc-aarch64-linux-gnu

# Install cross-compilation libraries
sudo dpkg --add-architecture arm64
sudo apt update
sudo apt install -y \
  libasound2-dev:arm64 \
  libssl-dev:arm64 \
  libsqlite3-dev:arm64
```

#### Configure Cargo for Cross-Compilation

Create or edit `~/.cargo/config.toml`:

```toml
[target.aarch64-unknown-linux-gnu]
linker = "aarch64-linux-gnu-gcc"
```

Set environment variables for pkg-config:

```bash
export PKG_CONFIG_SYSROOT_DIR=/usr/aarch64-linux-gnu
export PKG_CONFIG_PATH=/usr/lib/aarch64-linux-gnu/pkgconfig
```

#### Build

```bash
cd terminal
cargo build --release --target aarch64-unknown-linux-gnu
```

The binary is output to `target/aarch64-unknown-linux-gnu/release/zeiterfassung-terminal`.

#### Using `cross` (Simpler Alternative)

The [`cross`](https://github.com/cross-rs/cross) tool uses Docker to handle cross-compilation:

```bash
cargo install cross
cd terminal
cross build --release --target aarch64-unknown-linux-gnu
```

#### Transfer to Raspberry Pi

```bash
scp target/aarch64-unknown-linux-gnu/release/zeiterfassung-terminal \
  pi@raspberrypi:/usr/local/bin/
scp -r assets/ pi@raspberrypi:/opt/zeiterfassung/
scp -r locales/ pi@raspberrypi:/opt/zeiterfassung/
scp terminal.toml pi@raspberrypi:/etc/zeiterfassung/
```

---

## Configuration

### terminal.toml Reference

The terminal reads its configuration from `terminal.toml`. The default search path is:

1. `./terminal.toml` (current working directory)
2. `/etc/zeiterfassung/terminal.toml`

Below is a complete annotated configuration:

```toml
# ─── Display ───────────────────────────────────────────────────────────────────
[display]
# Display resolution (width x height). Common values:
#   "800x480"   — Official Raspberry Pi 7" touchscreen
#   "1024x600"  — Common 7" HDMI displays
#   "1280x720"  — 720p displays
#   "1920x1080" — Full HD displays
resolution = "1024x600"

# Run in fullscreen kiosk mode (no window decorations or taskbar)
fullscreen = true

# Display orientation: "landscape" or "portrait"
orientation = "landscape"

# Color theme: "dark" or "light"
theme = "dark"

# Font scaling factor. 1.0 = automatic based on resolution.
# Increase for larger text, decrease for smaller text.
font_scale = 1.0

# Seconds to show the confirmation screen after a successful clock-in/clock-out
# before returning to the idle/welcome screen.
idle_timeout_seconds = 8

# Seconds to show the error screen before returning to the idle/welcome screen.
error_timeout_seconds = 5

# ─── API ───────────────────────────────────────────────────────────────────────
[api]
# Backend API base URL. Must be reachable from the terminal's network.
base_url = "https://zeiterfassung.example.com/api"

# HTTP request timeout in seconds.
timeout_seconds = 10

# Number of retry attempts for failed API requests before falling back to
# offline buffering.
retry_attempts = 3

# Unique identifier for this terminal. Each physical terminal device MUST have a
# distinct terminal_id so that clock events can be attributed to the correct
# device. Example: "terminal-lobby", "terminal-warehouse-01".
terminal_id = "terminal-01"

# ─── Offline Buffering ────────────────────────────────────────────────────────
[offline]
# Path to the SQLite database used for buffering clock events when the backend
# is unreachable. The directory must be writable by the terminal process.
buffer_path = "/var/lib/zeiterfassung/buffer.db"

# How often (in seconds) to attempt syncing buffered events to the backend.
sync_interval_seconds = 30

# Maximum number of events to buffer. When the buffer is full, the oldest
# unsynced event is dropped to make room for new events.
max_buffer_size = 10000

# ─── RFID Reader ──────────────────────────────────────────────────────────────
[rfid]
# Input device path. Use "auto" to automatically detect a USB HID RFID reader,
# or specify a device path like "/dev/input/event0" for a specific reader.
input_device = "auto"

# Minimum time in milliseconds between accepting scans of the same tag.
# Prevents double-reads from a single badge tap.
debounce_ms = 500

# ─── Audio ─────────────────────────────────────────────────────────────────────
[audio]
# Enable or disable audible feedback sounds.
enabled = true

# Path to the WAV file played on successful clock-in/clock-out.
success_sound = "assets/sounds/success.wav"

# Path to the WAV file played on errors (unrecognized badge, server error).
error_sound = "assets/sounds/error.wav"

# Volume level from 0.0 (mute) to 1.0 (maximum).
volume = 0.7

# ─── Locale ────────────────────────────────────────────────────────────────────
[locale]
# UI language. Supported: "de" (German), "en" (English).
language = "de"

# ─── Company ───────────────────────────────────────────────────────────────────
[company]
# Company name displayed on the idle/welcome screen.
name = "Firma GmbH"

# Path to the company logo image (PNG recommended, displayed on idle screen).
logo_path = "assets/logo.png"
```

---

## Display Setup

### Screen Resolution

Configure the display resolution in `/boot/firmware/config.txt` (Raspberry Pi OS Bookworm):

```ini
# For HDMI displays — force a specific resolution
hdmi_group=2
hdmi_mode=87
hdmi_cvt=1024 600 60

# For the official DSI touchscreen — no hdmi config needed
# The touchscreen is auto-detected
```

Ensure the `resolution` value in `terminal.toml` matches the actual display resolution.

### Kiosk Mode

The terminal uses **cage** as a minimal Wayland compositor to run in kiosk mode (single fullscreen application, no desktop environment).

#### Install cage

```bash
sudo apt install -y cage
```

#### Test Kiosk Mode

```bash
cage -- /usr/local/bin/zeiterfassung-terminal
```

This starts the terminal application as the only visible application, with no window decorations, taskbar, or desktop.

### Auto-Start on Boot

Create a systemd service that launches cage with the terminal app on boot. See the [Systemd Service](#systemd-service) section below.

---

## RFID Reader Setup

### Verify the Reader is Detected

Plug in the USB RFID reader and verify it appears as an input device:

```bash
# List USB devices
lsusb

# List input devices
ls -la /dev/input/event*

# Monitor input events (scan a tag while this runs)
sudo cat /dev/input/event0
```

### Test with Standard Input

The terminal reads RFID tag IDs from standard input (the reader acts as a keyboard). Test this manually:

```bash
# Start the terminal in a normal terminal session
./zeiterfassung-terminal

# Type a tag ID and press Enter — the terminal will process it as a scan
```

### Permissions

The terminal process needs read access to input devices. Either:

1. Run the terminal as root (not recommended for production), or
2. Add the terminal user to the `input` group:

```bash
sudo usermod -aG input zeiterfassung
```

### Multiple Readers

If multiple USB input devices are connected, set `input_device` in `terminal.toml` to the specific device path instead of `"auto"`:

```toml
[rfid]
input_device = "/dev/input/event2"
```

Find the correct device with:

```bash
cat /proc/bus/input/devices
```

---

## Audio Configuration

### ALSA Setup

The terminal uses ALSA for audio output via the `rodio` crate.

```bash
# Test audio output
speaker-test -t wav -c 2 -l 1

# Adjust volume
alsamixer
```

### Headless Audio (No Desktop)

On Raspberry Pi OS Lite, the audio output may default to HDMI. To use the 3.5mm jack:

```bash
# Force audio output to 3.5mm jack
sudo raspi-config
# → System Options → Audio → Headphones
```

Or set in `/boot/firmware/config.txt`:

```ini
dtparam=audio=on
```

### Disabling Audio

If no speaker is connected, disable audio feedback in `terminal.toml`:

```toml
[audio]
enabled = false
```

---

## Systemd Service

Create a systemd service to start the terminal automatically on boot.

### Create Service File

```bash
sudo tee /etc/systemd/system/zeiterfassung-terminal.service > /dev/null << 'EOF'
[Unit]
Description=Zeiterfassung NFC Terminal
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=zeiterfassung
Group=zeiterfassung
WorkingDirectory=/opt/zeiterfassung
ExecStart=/usr/bin/cage -- /usr/local/bin/zeiterfassung-terminal
Restart=always
RestartSec=5

# Environment
Environment=WAYLAND_DISPLAY=wayland-0
Environment=XDG_RUNTIME_DIR=/run/user/1001

# Security hardening
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/lib/zeiterfassung
PrivateTmp=true

# Resource limits
MemoryMax=256M
CPUQuota=80%

[Install]
WantedBy=graphical.target
EOF
```

### Create the Service User

```bash
sudo useradd -r -s /usr/sbin/nologin -d /opt/zeiterfassung zeiterfassung
sudo usermod -aG input zeiterfassung
sudo usermod -aG audio zeiterfassung
sudo usermod -aG video zeiterfassung

# Set ownership
sudo chown -R zeiterfassung:zeiterfassung /opt/zeiterfassung
sudo chown -R zeiterfassung:zeiterfassung /var/lib/zeiterfassung
```

### Enable and Start

```bash
sudo systemctl daemon-reload
sudo systemctl enable zeiterfassung-terminal.service
sudo systemctl start zeiterfassung-terminal.service
```

### Check Status

```bash
sudo systemctl status zeiterfassung-terminal.service
sudo journalctl -u zeiterfassung-terminal.service -f
```

---

## Network Configuration

### Recommended: Wired Ethernet

For maximum reliability, use a wired Ethernet connection. Configure a static IP:

```bash
sudo nmcli connection modify "Wired connection 1" \
  ipv4.method manual \
  ipv4.addresses 192.168.1.100/24 \
  ipv4.gateway 192.168.1.1 \
  ipv4.dns "192.168.1.1"
sudo nmcli connection up "Wired connection 1"
```

### Firewall

The terminal only needs **outbound** HTTPS access to the backend API:

```bash
sudo apt install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw enable
```

### DNS Resolution

Ensure the backend hostname resolves correctly. For local networks, you may need to add an entry to `/etc/hosts`:

```
192.168.1.50  zeiterfassung.example.com
```

---

## Offline Mode & Data Buffering

When the backend API is unreachable, the terminal automatically buffers clock events in a local SQLite database.

### How It Works

1. Employee scans their badge.
2. The terminal attempts to send the clock event to the backend API.
3. If the API is unreachable (network error, timeout, server error):
   - The event is stored in the local SQLite buffer.
   - The terminal shows a confirmation screen with an **offline indicator**.
4. A background task periodically attempts to sync buffered events (configurable via `sync_interval_seconds`).
5. Events are synced in FIFO order (oldest first).
6. Successfully synced events are marked as synced in the buffer.

### Buffer Location

The SQLite database is stored at the path configured in `terminal.toml`:

```toml
[offline]
buffer_path = "/var/lib/zeiterfassung/buffer.db"
```

Ensure this directory exists and is writable by the terminal service user.

### Inspecting the Buffer

```bash
sqlite3 /var/lib/zeiterfassung/buffer.db

-- View pending (unsynced) events
SELECT * FROM buffered_events WHERE synced = 0 ORDER BY id ASC;

-- Count pending events
SELECT COUNT(*) FROM buffered_events WHERE synced = 0;
```

### Buffer Size Limits

When the buffer reaches `max_buffer_size`, the oldest unsynced event is dropped to make room. For most deployments, the default of 10,000 events provides several days of buffering capacity.

---

## Maintenance & Updates

### Updating the Terminal Binary

```bash
# Stop the service
sudo systemctl stop zeiterfassung-terminal.service

# Replace the binary
sudo cp zeiterfassung-terminal-new /usr/local/bin/zeiterfassung-terminal
sudo chmod +x /usr/local/bin/zeiterfassung-terminal

# Start the service
sudo systemctl start zeiterfassung-terminal.service
```

### Updating Configuration

```bash
# Edit configuration
sudo nano /etc/zeiterfassung/terminal.toml

# Restart to apply changes
sudo systemctl restart zeiterfassung-terminal.service
```

### Logs

```bash
# View recent logs
sudo journalctl -u zeiterfassung-terminal.service --since today

# Follow logs in real time
sudo journalctl -u zeiterfassung-terminal.service -f

# View logs for a specific time range
sudo journalctl -u zeiterfassung-terminal.service \
  --since "2024-01-15 08:00" --until "2024-01-15 18:00"
```

### Health Check

Create a simple health check script:

```bash
#!/bin/bash
# /opt/zeiterfassung/healthcheck.sh

# Check if service is running
if ! systemctl is-active --quiet zeiterfassung-terminal.service; then
    echo "CRITICAL: Terminal service is not running"
    exit 2
fi

# Check pending buffer count
PENDING=$(sqlite3 /var/lib/zeiterfassung/buffer.db \
  "SELECT COUNT(*) FROM buffered_events WHERE synced = 0;" 2>/dev/null || echo "-1")

if [ "$PENDING" -gt 100 ]; then
    echo "WARNING: $PENDING events pending sync"
    exit 1
fi

echo "OK: Service running, $PENDING events pending"
exit 0
```

### Automatic Updates via Script

```bash
#!/bin/bash
# /opt/zeiterfassung/update.sh
set -e

DOWNLOAD_URL="$1"
BINARY_PATH="/usr/local/bin/zeiterfassung-terminal"
BACKUP_PATH="/usr/local/bin/zeiterfassung-terminal.bak"

echo "Downloading new version..."
wget -q -O /tmp/zeiterfassung-terminal-new "$DOWNLOAD_URL"
chmod +x /tmp/zeiterfassung-terminal-new

echo "Stopping service..."
sudo systemctl stop zeiterfassung-terminal.service

echo "Backing up current binary..."
sudo cp "$BINARY_PATH" "$BACKUP_PATH"

echo "Installing new binary..."
sudo mv /tmp/zeiterfassung-terminal-new "$BINARY_PATH"

echo "Starting service..."
sudo systemctl start zeiterfassung-terminal.service

echo "Update complete. Verifying..."
sleep 2
if systemctl is-active --quiet zeiterfassung-terminal.service; then
    echo "Service is running. Update successful."
else
    echo "Service failed to start. Rolling back..."
    sudo cp "$BACKUP_PATH" "$BINARY_PATH"
    sudo systemctl start zeiterfassung-terminal.service
    echo "Rollback complete."
    exit 1
fi
```

---

## Troubleshooting

### Terminal Does Not Start

| Symptom | Cause | Solution |
|---------|-------|----------|
| `error while loading shared libraries: libasound.so.2` | Missing ALSA library | `sudo apt install libasound2-dev` |
| `error while loading shared libraries: libssl.so.3` | Missing OpenSSL | `sudo apt install libssl-dev` |
| `Failed to open display` | No Wayland compositor running | Use `cage` to launch the terminal, or ensure a display server is running |
| `Permission denied` on buffer database | Incorrect permissions on `/var/lib/zeiterfassung/` | `sudo chown zeiterfassung:zeiterfassung /var/lib/zeiterfassung` |
| Service starts and immediately stops | Configuration error | Check `journalctl -u zeiterfassung-terminal.service` for details |

### RFID Reader Not Working

| Symptom | Cause | Solution |
|---------|-------|----------|
| No input detected | Reader not recognized as HID device | Check `lsusb` and `dmesg` for connection errors |
| Double scans | Debounce too low | Increase `debounce_ms` in `terminal.toml` (try 1000) |
| Wrong characters received | Reader keyboard layout mismatch | Ensure the system keyboard layout matches the reader output (usually US layout) |
| `Permission denied` on `/dev/input/eventN` | User not in `input` group | `sudo usermod -aG input zeiterfassung` |

### Network / API Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| All events buffered (never synced) | Backend unreachable | Verify `base_url` in `terminal.toml`. Test with `curl <base_url>/health` |
| SSL certificate errors | Self-signed or expired certificate | Install the CA certificate on the Pi, or use a valid Let's Encrypt certificate |
| Timeout errors | Network latency or server overload | Increase `timeout_seconds` in `terminal.toml` |
| DNS resolution failure | Cannot resolve backend hostname | Add the hostname to `/etc/hosts` or fix DNS configuration |

### Display Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| Black screen | Wrong resolution or display not detected | Check `/boot/firmware/config.txt` HDMI settings |
| GUI too small / too large | Resolution mismatch | Ensure `resolution` in `terminal.toml` matches the actual display |
| Flickering display | Insufficient power or incorrect refresh rate | Use the official Raspberry Pi PSU. Adjust `hdmi_cvt` refresh rate |
| Touchscreen not working | Missing touchscreen drivers | Install the appropriate driver for your touchscreen model |

### Audio Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| No sound | Wrong audio output device | Run `sudo raspi-config` → Audio → select correct output |
| Sound distorted | Volume too high | Reduce `volume` in `terminal.toml` or adjust with `alsamixer` |
| `No audio device found` error | ALSA not configured | Install `libasound2-dev` and verify with `aplay -l` |
