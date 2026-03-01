import Foundation

class VacationService {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func getBalance(userId: String, year: Int) async throws -> VacationBalance {
        return try await client.get("/vacation/balance/\(userId)/\(year)")
    }

    func getRequests(userId: String, page: Int = 0, size: Int = 20) async throws -> PageResponse<VacationRequest> {
        return try await client.get("/vacation/requests/\(userId)?page=\(page)&size=\(size)")
    }
}
