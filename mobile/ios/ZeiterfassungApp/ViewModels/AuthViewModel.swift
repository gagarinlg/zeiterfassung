import Foundation
import Combine

@MainActor
class AuthViewModel: ObservableObject {
    @Published var isAuthenticated = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var currentUserId: String?

    private let authService: AuthService

    init(authService: AuthService = AuthService()) {
        self.authService = authService
        checkStoredSession()
    }

    private func checkStoredSession() {
        if let token = KeychainHelper.read(forKey: "access_token"), !token.isEmpty {
            currentUserId = KeychainHelper.read(forKey: "user_id")
            isAuthenticated = true
        }
    }

    func login(email: String, password: String) async {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = NSLocalizedString("auth_login_error", comment: "")
            return
        }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            let response = try await authService.login(email: email, password: password)
            KeychainHelper.save(response.accessToken, forKey: "access_token")
            KeychainHelper.save(response.refreshToken, forKey: "refresh_token")
            KeychainHelper.save(response.user.id, forKey: "user_id")
            currentUserId = response.user.id
            isAuthenticated = true
        } catch {
            errorMessage = NSLocalizedString("auth_login_error", comment: "")
        }
    }

    func logout() {
        Task {
            try? await authService.logout()
        }
        KeychainHelper.delete(forKey: "access_token")
        KeychainHelper.delete(forKey: "refresh_token")
        KeychainHelper.delete(forKey: "user_id")
        currentUserId = nil
        isAuthenticated = false
    }
}

