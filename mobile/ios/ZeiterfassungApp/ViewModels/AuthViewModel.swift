import Foundation
import Combine

@MainActor
class AuthViewModel: ObservableObject {
    @Published var isAuthenticated = false
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let authService: AuthService

    init(authService: AuthService = AuthService()) {
        self.authService = authService
        checkStoredSession()
    }

    private func checkStoredSession() {
        // TODO: Replace UserDefaults with iOS Keychain for secure token storage
        if let token = UserDefaults.standard.string(forKey: "access_token"), !token.isEmpty {
            isAuthenticated = true
        }
    }

    func login(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            let response = try await authService.login(email: email, password: password)
            // TODO: Replace UserDefaults with iOS Keychain for secure token storage
            UserDefaults.standard.set(response.tokens.accessToken, forKey: "access_token")
            UserDefaults.standard.set(response.tokens.refreshToken, forKey: "refresh_token")
            isAuthenticated = true
        } catch {
            errorMessage = NSLocalizedString("auth_login_error", comment: "")
        }
    }

    func logout() {
        UserDefaults.standard.removeObject(forKey: "access_token")
        UserDefaults.standard.removeObject(forKey: "refresh_token")
        isAuthenticated = false
    }
}
