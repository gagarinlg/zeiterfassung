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

struct AuthTokens: Codable {
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int
}

struct LoginResponse: Codable {
    let tokens: AuthTokens
    let user: User
}
