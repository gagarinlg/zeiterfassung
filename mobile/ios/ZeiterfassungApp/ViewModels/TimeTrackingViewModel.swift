import Foundation

@MainActor
class TimeTrackingViewModel: ObservableObject {
    @Published var trackingStatus: TrackingStatusResponse?
    @Published var isLoading = false
    @Published var isActionLoading = false
    @Published var errorMessage: String?
    @Published var actionError: String?

    private let timeService: TimeService

    init(timeService: TimeService = TimeService()) {
        self.timeService = timeService
    }

    func loadStatus(userId: String) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            trackingStatus = try await timeService.getTrackingStatus(userId: userId)
        } catch {
            errorMessage = NSLocalizedString("common_error", comment: "")
        }
    }

    func clockIn(userId: String) async {
        await performAction(userId: userId) { try await self.timeService.clockIn() }
    }

    func clockOut(userId: String) async {
        await performAction(userId: userId) { try await self.timeService.clockOut() }
    }

    func startBreak(userId: String) async {
        await performAction(userId: userId) { try await self.timeService.startBreak() }
    }

    func endBreak(userId: String) async {
        await performAction(userId: userId) { try await self.timeService.endBreak() }
    }

    private func performAction(userId: String, action: @escaping () async throws -> TimeEntry) async {
        isActionLoading = true
        actionError = nil
        defer { isActionLoading = false }
        do {
            _ = try await action()
            await loadStatus(userId: userId)
        } catch {
            actionError = NSLocalizedString("common_error", comment: "")
        }
    }

    func clearActionError() {
        actionError = nil
    }
}
