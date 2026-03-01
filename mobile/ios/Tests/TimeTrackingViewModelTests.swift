import XCTest
@testable import ZeiterfassungCore

// MARK: - Mock

final class MockTimeService: TimeServiceProtocol {
    var statusResult: Result<TrackingStatusResponse, Error> = .success(makeClockedOutStatus())
    var clockInResult: Result<TimeEntry, Error> = .success(makeTimeEntry())
    var clockOutResult: Result<TimeEntry, Error> = .success(makeTimeEntry())
    var startBreakResult: Result<TimeEntry, Error> = .success(makeTimeEntry())
    var endBreakResult: Result<TimeEntry, Error> = .success(makeTimeEntry())

    var clockInCalled = false
    var clockOutCalled = false
    var startBreakCalled = false
    var endBreakCalled = false

    func getTrackingStatus(userId: String) async throws -> TrackingStatusResponse {
        return try statusResult.get()
    }

    func clockIn() async throws -> TimeEntry {
        clockInCalled = true
        return try clockInResult.get()
    }

    func clockOut() async throws -> TimeEntry {
        clockOutCalled = true
        return try clockOutResult.get()
    }

    func startBreak() async throws -> TimeEntry {
        startBreakCalled = true
        return try startBreakResult.get()
    }

    func endBreak() async throws -> TimeEntry {
        endBreakCalled = true
        return try endBreakResult.get()
    }
}

// MARK: - Tests

@MainActor
final class TimeTrackingViewModelTests: XCTestCase {
    private var mockService: MockTimeService!
    private var viewModel: TimeTrackingViewModel!

    override func setUp() {
        super.setUp()
        mockService = MockTimeService()
        viewModel = TimeTrackingViewModel(timeService: mockService)
    }

    override func tearDown() {
        viewModel = nil
        mockService = nil
        super.tearDown()
    }

    // MARK: - Status loading

    func testLoadStatusSuccess() async {
        let status = makeClockedInStatus()
        mockService.statusResult = .success(status)

        await viewModel.loadStatus(userId: "u1")

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertEqual(viewModel.trackingStatus?.status, "CLOCKED_IN")
        XCTAssertNil(viewModel.errorMessage)
    }

    func testLoadStatusFailureSetsError() async {
        mockService.statusResult = .failure(URLError(.notConnectedToInternet))

        await viewModel.loadStatus(userId: "u1")

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    // MARK: - Clock In

    func testClockInCallsServiceAndRefreshesStatus() async {
        let clockedInStatus = makeClockedInStatus()

        mockService.statusResult = .success(makeClockedOutStatus())

        // First load
        await viewModel.loadStatus(userId: "u1")
        XCTAssertEqual(viewModel.trackingStatus?.status, "CLOCKED_OUT")

        // Prepare updated status for post-action refresh
        mockService.statusResult = .success(clockedInStatus)

        await viewModel.clockIn(userId: "u1")

        XCTAssertTrue(mockService.clockInCalled)
        XCTAssertEqual(viewModel.trackingStatus?.status, "CLOCKED_IN")
        XCTAssertFalse(viewModel.isActionLoading)
        XCTAssertNil(viewModel.actionError)
    }

    func testClockInFailureSetsActionError() async {
        mockService.clockInResult = .failure(URLError(.timedOut))

        await viewModel.clockIn(userId: "u1")

        XCTAssertTrue(mockService.clockInCalled)
        XCTAssertNotNil(viewModel.actionError)
        XCTAssertFalse(viewModel.isActionLoading)
    }

    // MARK: - Clock Out

    func testClockOutCallsService() async {
        mockService.statusResult = .success(makeClockedOutStatus())

        await viewModel.clockOut(userId: "u1")

        XCTAssertTrue(mockService.clockOutCalled)
        XCTAssertFalse(viewModel.isActionLoading)
    }

    func testClockOutFailureSetsActionError() async {
        mockService.clockOutResult = .failure(URLError(.timedOut))

        await viewModel.clockOut(userId: "u1")

        XCTAssertNotNil(viewModel.actionError)
    }

    // MARK: - Break

    func testStartBreakCallsService() async {
        mockService.statusResult = .success(makeOnBreakStatus())

        await viewModel.startBreak(userId: "u1")

        XCTAssertTrue(mockService.startBreakCalled)
        XCTAssertFalse(viewModel.isActionLoading)
    }

    func testEndBreakCallsService() async {
        mockService.statusResult = .success(makeClockedInStatus())

        await viewModel.endBreak(userId: "u1")

        XCTAssertTrue(mockService.endBreakCalled)
        XCTAssertFalse(viewModel.isActionLoading)
    }

    // MARK: - Clear error

    func testClearActionErrorResetsError() async {
        mockService.clockInResult = .failure(URLError(.timedOut))
        await viewModel.clockIn(userId: "u1")
        XCTAssertNotNil(viewModel.actionError)

        viewModel.clearActionError()

        XCTAssertNil(viewModel.actionError)
    }
}

// MARK: - Factories

private func makeClockedOutStatus() -> TrackingStatusResponse {
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

private func makeClockedInStatus() -> TrackingStatusResponse {
    TrackingStatusResponse(
        status: "CLOCKED_IN",
        clockedInSince: "2026-03-01T08:00:00Z",
        breakStartedAt: nil,
        elapsedWorkMinutes: 120,
        elapsedBreakMinutes: 0,
        todayWorkMinutes: 120,
        todayBreakMinutes: 0
    )
}

private func makeOnBreakStatus() -> TrackingStatusResponse {
    TrackingStatusResponse(
        status: "ON_BREAK",
        clockedInSince: "2026-03-01T08:00:00Z",
        breakStartedAt: "2026-03-01T10:00:00Z",
        elapsedWorkMinutes: 120,
        elapsedBreakMinutes: 10,
        todayWorkMinutes: 120,
        todayBreakMinutes: 10
    )
}

private func makeTimeEntry() -> TimeEntry {
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
