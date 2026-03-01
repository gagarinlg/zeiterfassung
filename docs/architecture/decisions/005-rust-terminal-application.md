# ADR 005: Rust for Raspberry Pi Terminal Application

## Status

Accepted

## Context

The Raspberry Pi 5 terminal runs a kiosk-mode GUI application that processes RFID badge scans, displays clock-in/out confirmation, and buffers events during offline periods. Reliability, low resource usage, and fast startup are critical.

## Decision

We use **Rust** with the **iced 0.12** GUI framework for the terminal application.

### Key design choices

- **iced Application**: State machine pattern (Idle → Loading → ClockIn/ClockOut/OfflineConfirm/Error → Idle) with auto-return timers.
- **RFID input**: The RFID reader emulates a USB keyboard; input is read via subscription polling at 50ms intervals.
- **Offline buffering**: Clock events are stored in a local **SQLite** database when the backend is unreachable, synced in FIFO order when connectivity is restored.
- **Configuration**: `terminal.toml` file for display resolution, API endpoint, RFID device, audio settings, and locale.
- **Audio feedback**: `rodio` crate for success/error sounds.

### Why Rust over Python

- Compiled binary with no runtime dependency.
- Memory safety without garbage collection — suitable for long-running kiosk operation.
- Fast startup time (~100ms vs ~1s for Python + GUI framework).
- Cross-compilation support for ARM (Raspberry Pi).

## Consequences

- Developers need Rust experience to maintain the terminal codebase.
- Compilation requires a Rust toolchain; cross-compilation for ARM uses `cross`.
- The iced framework is less mature than GTK/Qt but provides a simple, Elm-inspired API.
- SQLite buffer ensures no clock events are lost during network outages.
