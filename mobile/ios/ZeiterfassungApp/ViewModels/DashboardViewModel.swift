import Foundation

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var user: User?
    @Published var trackingStatus: TrackingStatusResponse?
    @Published var vacationBalance: VacationBalance?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let timeService: TimeService
    private let vacationService: VacationService

    init(timeService: TimeService = TimeService(), vacationService: VacationService = VacationService()) {
        self.timeService = timeService
        self.vacationService = vacationService
    }

    func load(userId: String) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                do {
                    let status = try await self.timeService.getTrackingStatus(userId: userId)
                    await MainActor.run { self.trackingStatus = status }
                } catch {
                    await MainActor.run { self.errorMessage = NSLocalizedString("common_error", comment: "") }
                }
            }
            group.addTask {
                do {
                    let year = Calendar.current.component(.year, from: Date())
                    let balance = try await self.vacationService.getBalance(userId: userId, year: year)
                    await MainActor.run { self.vacationBalance = balance }
                } catch {
                    // Non-fatal: leave vacationBalance nil
                }
            }
        }
    }
}
