import Foundation

protocol AuthServiceProtocol {
    func login(email: String, password: String) async throws -> LoginResponse
    func logout() async throws
}

protocol TimeServiceProtocol {
    func getTrackingStatus(userId: String) async throws -> TrackingStatusResponse
    func clockIn() async throws -> TimeEntry
    func clockOut() async throws -> TimeEntry
    func startBreak() async throws -> TimeEntry
    func endBreak() async throws -> TimeEntry
}

protocol VacationServiceProtocol {
    func getBalance(userId: String, year: Int) async throws -> VacationBalance
    func getRequests(userId: String, page: Int, size: Int) async throws -> PageResponse<VacationRequest>
}
