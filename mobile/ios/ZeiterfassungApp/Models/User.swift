import Foundation

struct User: Codable, Identifiable {
    let id: String
    let email: String
    let firstName: String
    let lastName: String
    let employeeNumber: String?
    let phone: String?
    let photoUrl: String?
    let managerId: String?
    let isActive: Bool
    let roles: [String]
    let permissions: [String]

    var fullName: String {
        "\(firstName) \(lastName)"
    }
}

// Flat structure matching the backend response
struct LoginResponse: Codable {
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int
    let user: User
}

struct TrackingStatusResponse: Codable {
    let status: String
    let clockedInSince: String?
    let breakStartedAt: String?
    let elapsedWorkMinutes: Int
    let elapsedBreakMinutes: Int
    let todayWorkMinutes: Int
    let todayBreakMinutes: Int
}

struct VacationBalance: Codable, Identifiable {
    let id: String
    let userId: String
    let year: Int
    let totalDays: Double
    let usedDays: Double
    let carriedOverDays: Double
    let remainingDays: Double
}

