import XCTest
@testable import ZeiterfassungCore

// MARK: - Mock

final class MockAuthService: AuthServiceProtocol {
    var loginResult: Result<LoginResponse, Error> = .failure(URLError(.unknown))
    var logoutCalled = false

    func login(email: String, password: String) async throws -> LoginResponse {
        return try loginResult.get()
    }

    func logout() async throws {
        logoutCalled = true
    }
}

// MARK: - Tests

@MainActor
final class AuthViewModelTests: XCTestCase {
    private var mockService: MockAuthService!
    private var viewModel: AuthViewModel!

    override func setUp() {
        super.setUp()
        // Clean up any stale keychain entries from previous test runs
        KeychainHelper.delete(forKey: "access_token")
        KeychainHelper.delete(forKey: "refresh_token")
        KeychainHelper.delete(forKey: "user_id")
        mockService = MockAuthService()
        viewModel = AuthViewModel(authService: mockService)
    }

    override func tearDown() {
        // Remove any keychain entries written during the test
        KeychainHelper.delete(forKey: "access_token")
        KeychainHelper.delete(forKey: "refresh_token")
        KeychainHelper.delete(forKey: "user_id")
        viewModel = nil
        mockService = nil
        super.tearDown()
    }

    // MARK: - Initial state

    func testInitialStateNotAuthenticated() {
        // No keychain token set → should not be authenticated
        XCTAssertFalse(viewModel.isAuthenticated)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertNil(viewModel.currentUserId)
    }

    // MARK: - Login success

    func testLoginSuccessSetsAuthenticated() async throws {
        let user = makeUser()
        mockService.loginResult = .success(LoginResponse(
            accessToken: "tok",
            refreshToken: "ref",
            expiresIn: 900,
            user: user
        ))

        await viewModel.login(email: "alice@example.com", password: "secret")

        XCTAssertTrue(viewModel.isAuthenticated)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertEqual(viewModel.currentUserId, user.id)
    }

    // MARK: - Login failure

    func testLoginFailureSetsErrorMessage() async {
        mockService.loginResult = .failure(URLError(.notConnectedToInternet))

        await viewModel.login(email: "alice@example.com", password: "wrong")

        XCTAssertFalse(viewModel.isAuthenticated)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    // MARK: - Validation

    func testLoginEmptyEmailShowsErrorWithoutCallingService() async {
        // Provide a success result to ensure we detect any stray service call
        mockService.loginResult = .success(LoginResponse(
            accessToken: "tok", refreshToken: "ref", expiresIn: 900, user: makeUser()
        ))

        await viewModel.login(email: "", password: "secret")

        XCTAssertFalse(viewModel.isAuthenticated)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    func testLoginEmptyPasswordShowsErrorWithoutCallingService() async {
        mockService.loginResult = .success(LoginResponse(
            accessToken: "tok", refreshToken: "ref", expiresIn: 900, user: makeUser()
        ))

        await viewModel.login(email: "alice@example.com", password: "")

        XCTAssertFalse(viewModel.isAuthenticated)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    // MARK: - Logout

    func testLogoutClearsAuthentication() async {
        // Simulate logged-in state
        let user = makeUser()
        mockService.loginResult = .success(LoginResponse(
            accessToken: "tok", refreshToken: "ref", expiresIn: 900, user: user
        ))
        await viewModel.login(email: "alice@example.com", password: "secret")
        XCTAssertTrue(viewModel.isAuthenticated)

        // Logout — synchronous side-effects happen immediately
        viewModel.logout()

        XCTAssertFalse(viewModel.isAuthenticated)
        XCTAssertNil(viewModel.currentUserId)
    }

    // MARK: - Helpers

    private func makeUser() -> User {
        User(
            id: "u1",
            email: "alice@example.com",
            firstName: "Alice",
            lastName: "Smith",
            employeeNumber: nil,
            phone: nil,
            photoUrl: nil,
            managerId: nil,
            isActive: true,
            roles: ["EMPLOYEE"],
            permissions: []
        )
    }
}
