use chrono::{DateTime, Utc};
use log::{info, warn};
use rusqlite::{params, Connection, Result as SqliteResult};
use serde::{Deserialize, Serialize};
use std::path::Path;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BufferedEvent {
    pub id: Option<i64>,
    pub rfid_tag_id: String,
    pub terminal_id: String,
    pub timestamp: DateTime<Utc>,
    pub synced: bool,
}

pub struct EventBuffer {
    conn: Connection,
    max_size: u32,
}

impl EventBuffer {
    pub fn new(db_path: &str, max_size: u32) -> Result<Self, Box<dyn std::error::Error>> {
        let path = Path::new(db_path);
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }

        let conn = Connection::open(db_path)?;
        let buffer = Self { conn, max_size };
        buffer.initialize()?;
        Ok(buffer)
    }

    fn initialize(&self) -> SqliteResult<()> {
        self.conn.execute_batch(
            "CREATE TABLE IF NOT EXISTS buffered_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rfid_tag_id TEXT NOT NULL,
                terminal_id TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                synced INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT (datetime('now'))
            );
            CREATE INDEX IF NOT EXISTS idx_buffered_events_synced ON buffered_events(synced);",
        )?;
        info!("Event buffer database initialized");
        Ok(())
    }

    pub fn push(&self, rfid_tag_id: &str, terminal_id: &str) -> SqliteResult<i64> {
        let count: u32 = self.conn.query_row(
            "SELECT COUNT(*) FROM buffered_events WHERE synced = 0",
            [],
            |row| row.get(0),
        )?;

        if count >= self.max_size {
            warn!(
                "Event buffer is full ({} events). Dropping oldest event.",
                self.max_size
            );
            self.conn.execute(
                "DELETE FROM buffered_events WHERE id = (SELECT MIN(id) FROM buffered_events WHERE synced = 0)",
                [],
            )?;
        }

        let now = Utc::now().to_rfc3339();
        self.conn.execute(
            "INSERT INTO buffered_events (rfid_tag_id, terminal_id, timestamp) VALUES (?1, ?2, ?3)",
            params![rfid_tag_id, terminal_id, now],
        )?;

        Ok(self.conn.last_insert_rowid())
    }

    pub fn get_pending(&self) -> SqliteResult<Vec<BufferedEvent>> {
        let mut stmt = self.conn.prepare(
            "SELECT id, rfid_tag_id, terminal_id, timestamp, synced FROM buffered_events WHERE synced = 0 ORDER BY id ASC",
        )?;

        let events = stmt
            .query_map([], |row| {
                let ts_str: String = row.get(3)?;
                let timestamp = DateTime::parse_from_rfc3339(&ts_str)
                    .map(|dt| dt.with_timezone(&Utc))
                    .unwrap_or_else(|_| Utc::now());

                Ok(BufferedEvent {
                    id: Some(row.get(0)?),
                    rfid_tag_id: row.get(1)?,
                    terminal_id: row.get(2)?,
                    timestamp,
                    synced: row.get::<_, i32>(4)? != 0,
                })
            })?
            .collect::<SqliteResult<Vec<_>>>()?;

        Ok(events)
    }

    pub fn mark_synced(&self, id: i64) -> SqliteResult<()> {
        self.conn.execute(
            "UPDATE buffered_events SET synced = 1 WHERE id = ?1",
            params![id],
        )?;
        Ok(())
    }

    pub fn pending_count(&self) -> SqliteResult<u32> {
        self.conn.query_row(
            "SELECT COUNT(*) FROM buffered_events WHERE synced = 0",
            [],
            |row| row.get(0),
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_buffer() -> EventBuffer {
        EventBuffer::new(":memory:", 10).expect("failed to create in-memory buffer")
    }

    #[test]
    fn test_push_and_pending_count() {
        let buf = make_buffer();
        assert_eq!(buf.pending_count().unwrap(), 0);

        buf.push("TAG001", "terminal-1").unwrap();
        buf.push("TAG002", "terminal-1").unwrap();
        assert_eq!(buf.pending_count().unwrap(), 2);
    }

    #[test]
    fn test_get_pending_returns_unsynced_events() {
        let buf = make_buffer();
        buf.push("TAG001", "terminal-1").unwrap();
        buf.push("TAG002", "terminal-2").unwrap();

        let pending = buf.get_pending().unwrap();
        assert_eq!(pending.len(), 2);
        assert_eq!(pending[0].rfid_tag_id, "TAG001");
        assert_eq!(pending[1].rfid_tag_id, "TAG002");
        assert!(!pending[0].synced);
    }

    #[test]
    fn test_mark_synced_removes_from_pending() {
        let buf = make_buffer();
        let id = buf.push("TAG001", "terminal-1").unwrap();

        assert_eq!(buf.pending_count().unwrap(), 1);
        buf.mark_synced(id).unwrap();
        assert_eq!(buf.pending_count().unwrap(), 0);

        let pending = buf.get_pending().unwrap();
        assert!(pending.is_empty());
    }

    #[test]
    fn test_max_size_enforcement() {
        let buf = EventBuffer::new(":memory:", 3).expect("failed to create buffer");

        buf.push("TAG001", "terminal-1").unwrap();
        buf.push("TAG002", "terminal-1").unwrap();
        buf.push("TAG003", "terminal-1").unwrap();
        // This should drop TAG001 (oldest)
        buf.push("TAG004", "terminal-1").unwrap();

        assert_eq!(buf.pending_count().unwrap(), 3);
        let pending = buf.get_pending().unwrap();
        // Oldest event dropped, newer ones retained
        let tags: Vec<&str> = pending.iter().map(|e| e.rfid_tag_id.as_str()).collect();
        assert!(
            !tags.contains(&"TAG001"),
            "oldest event should have been dropped"
        );
        assert!(tags.contains(&"TAG004"));
    }

    #[test]
    fn test_push_returns_row_id() {
        let buf = make_buffer();
        let id1 = buf.push("TAG001", "terminal-1").unwrap();
        let id2 = buf.push("TAG002", "terminal-1").unwrap();
        assert!(id2 > id1);
    }
}
