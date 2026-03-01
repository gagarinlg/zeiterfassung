import XCTest
@testable import ZeiterfassungCore

// MARK: - Mock

final class MockVacationService: VacationServiceProtocol {
    var balanceResult: Result<VacationBalance, Error> = .success(makeBalance())
    var requestsResult: Result<PageResponse<VacationRequest>, Error> = .success(makeEmptyPage())

    func getBalance(userId: String, year: Int) async throws -> VacationBalance {
        return try balanceResult.get()
    }

    func getRequests(userId: String, page: Int, size: Int) async throws -> PageResponse<VacationRequest> {
        return try requestsResult.get()
    }
}

// MARK: - Tests

@MainActor
final class VacationViewModelTests: XCTestCase {
    private var mockService: MockVacationService!
    private var viewModel: VacationViewModel!

    override func setUp() {
        super.setUp()
        mockService = MockVacationService()
        viewModel = VacationViewModel(vacationService: mockService)
    }

    override func tearDown() {
        viewModel = nil
        mockService = nil
        super.tearDown()
    }

    // MARK: - Initial state

    func testInitialStateIsEmpty() {
        XCTAssertNil(viewModel.balance)
        XCTAssertTrue(viewModel.requests.isEmpty)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
    }

    // MARK: - Load success

    func testLoadFetchesBalanceAndRequests() async {
        let balance = makeBalance()
        let request = makeVacationRequest()
        mockService.balanceResult = .success(balance)
        mockService.requestsResult = .success(PageResponse(
            content: [request],
            totalElements: 1,
            totalPages: 1,
            pageNumber: 0,
            pageSize: 20
        ))

        await viewModel.load(userId: "u1")

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertNotNil(viewModel.balance)
        XCTAssertEqual(viewModel.balance?.remainingDays ?? 0, balance.remainingDays, accuracy: 0.001)
        XCTAssertEqual(viewModel.requests.count, 1)
        XCTAssertEqual(viewModel.requests.first?.id, request.id)
    }

    func testLoadBalanceFailureSetsError() async {
        mockService.balanceResult = .failure(URLError(.notConnectedToInternet))

        await viewModel.load(userId: "u1")

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    func testLoadRequestsFailureKeepsEmptyRequestsButNoError() async {
        mockService.requestsResult = .failure(URLError(.notConnectedToInternet))

        await viewModel.load(userId: "u1")

        // Balance succeeds → no error message
        XCTAssertNil(viewModel.errorMessage)
        // Requests failed → empty
        XCTAssertTrue(viewModel.requests.isEmpty)
    }

    func testLoadSetsIsLoadingDuringFetch() async {
        // isLoading becomes false after load completes
        await viewModel.load(userId: "u1")
        XCTAssertFalse(viewModel.isLoading)
    }

    func testLoadWithEmptyRequestsPage() async {
        mockService.requestsResult = .success(makeEmptyPage())

        await viewModel.load(userId: "u1")

        XCTAssertTrue(viewModel.requests.isEmpty)
    }
}

// MARK: - Factories

private func makeBalance() -> VacationBalance {
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

private func makeEmptyPage() -> PageResponse<VacationRequest> {
    PageResponse(
        content: [],
        totalElements: 0,
        totalPages: 0,
        pageNumber: 0,
        pageSize: 20
    )
}

private func makeVacationRequest() -> VacationRequest {
    VacationRequest(
        id: "vr1",
        userId: "u1",
        startDate: "2026-04-01",
        endDate: "2026-04-05",
        isHalfDayStart: false,
        isHalfDayEnd: false,
        totalDays: 5.0,
        status: .pending,
        approvedBy: nil,
        rejectionReason: nil,
        notes: nil,
        createdAt: "2026-03-01T10:00:00Z"
    )
}
