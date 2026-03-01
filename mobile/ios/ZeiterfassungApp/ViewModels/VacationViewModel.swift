import Foundation

@MainActor
class VacationViewModel: ObservableObject {
    @Published var balance: VacationBalance?
    @Published var requests: [VacationRequest] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let vacationService: VacationServiceProtocol

    init(vacationService: VacationServiceProtocol = VacationService()) {
        self.vacationService = vacationService
    }

    func load(userId: String) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        let year = Calendar.current.component(.year, from: Date())
        do {
            balance = try await vacationService.getBalance(userId: userId, year: year)
        } catch {
            errorMessage = NSLocalizedString("common_error", comment: "")
        }

        do {
            let page = try await vacationService.getRequests(userId: userId, page: 0, size: 20)
            requests = page.content
        } catch {
            // Non-fatal: leave requests empty
        }
    }
}
