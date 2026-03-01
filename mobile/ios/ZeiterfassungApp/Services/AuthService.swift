import Foundation

class AuthService: AuthServiceProtocol {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    struct LoginRequest: Encodable {
        let email: String
        let password: String
    }

    struct EmptyBody: Encodable {}
    struct EmptyResponse: Decodable {}

    func login(email: String, password: String) async throws -> LoginResponse {
        return try await client.post("/auth/login", body: LoginRequest(email: email, password: password))
    }

    func logout() async throws {
        let _: EmptyResponse = try await client.post("/auth/logout", body: EmptyBody())
    }
}

