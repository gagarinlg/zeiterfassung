import Foundation

class TimeService {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
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

    private struct EmptyBody: Encodable {}
}
