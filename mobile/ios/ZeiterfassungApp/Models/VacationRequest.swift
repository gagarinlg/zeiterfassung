import Foundation

struct VacationRequest: Codable, Identifiable {
    let id: String
    let userId: String
    let startDate: String
    let endDate: String
    let isHalfDayStart: Bool
    let isHalfDayEnd: Bool
    let totalDays: Double
    let status: Status
    let approvedBy: String?
    let rejectionReason: String?
    let notes: String?
    let createdAt: String

    enum Status: String, Codable {
        case pending = "PENDING"
        case approved = "APPROVED"
        case rejected = "REJECTED"
        case cancelled = "CANCELLED"
    }
}

struct PageResponse<T: Codable>: Codable {
    let content: [T]
    let totalElements: Int
    let totalPages: Int
    let pageNumber: Int
    let pageSize: Int
}
