import Foundation

struct VacationRequest: Codable, Identifiable {
    let id: String
    let userId: String
    let startDate: Date
    let endDate: Date
    let status: Status
    let notes: String?
    let isHalfDay: Bool

    enum Status: String, Codable {
        case pending = "PENDING"
        case approved = "APPROVED"
        case rejected = "REJECTED"
    }
}
