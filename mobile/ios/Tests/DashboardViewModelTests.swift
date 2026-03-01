import XCTest
@testable import ZeiterfassungCore

// MARK: - Mocks (reuse MockTimeService and MockVacationService approach)

private final class MockDashTimeService: TimeServiceProtocol {
    var statusResult: Result<TrackingStatusResponse, Error> = .success(makeDashClockedOutStatus())

    func getTrackingStatus(userId: String) async throws -> TrackingStatusResponse {
        return try statusResult.get()
    }

    func clockIn() async throws -> TimeEntry { makeEntry() }
    func clockOut() async throws -> TimeEntry { makeEntry() }
    func startBreak() async throws -> TimeEntry { makeEntry() }
    func endBreak() async throws -> TimeEntry { makeEntry() }
}

private final class MockDashVacationService: VacationServiceProtocol {
    var balanceResult: Result<VacationBalance, Error> = .success(makeDashBalance())

    func getBalance(userId: String, year: Int) async throws -> VacationBalance {
        return try balanceResult.get()
    }

    func getRequests(userId: String, page: Int, size: Int) async throws -> PageResponse<VacationRequest> {
        return PageResponse(content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 20)
    }
}

// MARK: - Tests

@MainActor
final class DashboardViewModelTests: XCTestCase {
    private var mockTimeService: MockDashTimeService!
    private var mockVacationService: MockDashVacationService!
    private var viewModel: DashboardViewModel!

    override func setUp() {
        super.setUp()
        mockTimeService = MockDashTimeService()
        mockVacationService = MockDashVacationService()
        viewModel = DashboardViewModel(
            timeService: mockTimeService,
            vacationService: mockVacationService
        )
    }

    override func tearDown() {
        viewModel = nil
        mockTimeService = nil
        mockVacationService = nil
        super.tearDown()
    }

    // MARK: - Initial state

    func testInitialStateIsEmpty() {
        XCTAssertNil(viewModel.trackingStatus)
        XCTAssertNil(viewModel.vacationBalance)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
    }

    // MARK: - Load success

    func testLoadFetchesStatusAndBalance() async {
        let status = TrackingStatusResponse(
            status: "CLOCKED_IN",
            clockedInSince: "2026-03-01T08:00:00Z",
            breakStartedAt: nil,
            elapsedWorkMinutes: 60,
            elapsedBreakMinutes: 0,
            todayWorkMinutes: 60,
            todayBreakMinutes: 0
        )
        mockTimeService.statusResult = .success(status)

        await viewModel.load(userId: "u1")

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertEqual(viewModel.trackingStatus?.status, "CLOCKED_IN")
        XCTAssertNotNil(viewModel.vacationBalance)
    }

    func testLoadStatusFailureSetsError() async {
        mockTimeService.statusResult = .failure(URLError(.notConnectedToInternet))

        await viewModel.load(userId: "u1")

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    func testLoadVacationBalanceFailureDoesNotSetError() async {
        mockVacationService.balanceResult = .failure(URLError(.notConnectedToInternet))

        await viewModel.load(userId: "u1")

        // Status loads fine, balance fails silently
        XCTAssertNil(viewModel.vacationBalance)
        XCTAssertNil(viewModel.errorMessage)
    }

    func testLoadCompletesWithIsLoadingFalse() async {
        await viewModel.load(userId: "u1")
        XCTAssertFalse(viewModel.isLoading)
    }
}

// MARK: - Factories

private func makeDashClockedOutStatus() -> TrackingStatusResponse {
    TrackingStatusResponse(
        status: "CLOCKED_OUT",
        clockedInSince: nil,
        breakStartedAt: nil,
        elapsedWorkMinutes: 0,
        elapsedBreakMinutes: 0,
        todayWorkMinutes: 0,
        todayBreakMinutes: 0
    )
}

private func makeDashBalance() -> VacationBalance {
    VacationBalance(
        id: "vb1",
        userId: "u1",
        year: 2026,
        totalDays: 30.0,
        usedDays: 5.0,
        carriedOverDays: 0.0,
        remainingDays: 25.0
    )
}

private func makeEntry() -> TimeEntry {
    TimeEntry(
        id: "te1",
        userId: "u1",
        entryType: .clockIn,
        timestamp: Date(),
        source: .mobile,
        notes: nil,
        isModified: false
    )
}
