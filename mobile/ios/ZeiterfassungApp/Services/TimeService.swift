import Foundation

class TimeService: TimeServiceProtocol {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    private struct EmptyBody: Encodable {}

    func getTrackingStatus(userId: String) async throws -> TrackingStatusResponse {
        return try await client.get("/time-entries/status/\(userId)")
    }

    func getTimeEntries(userId: String) async throws -> [TimeEntry] {
        return try await client.get("/time-entries?userId=\(userId)")
    }

    func clockIn() async throws -> TimeEntry {
        return try await client.post("/time-entries/clock-in", body: EmptyBody())
    }

    func clockOut() async throws -> TimeEntry {
        return try await client.post("/time-entries/clock-out", body: EmptyBody())
    }

    func startBreak() async throws -> TimeEntry {
        return try await client.post("/time-entries/break/start", body: EmptyBody())
    }

    func endBreak() async throws -> TimeEntry {
        return try await client.post("/time-entries/break/end", body: EmptyBody())
    }
}

