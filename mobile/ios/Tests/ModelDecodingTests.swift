import XCTest
@testable import ZeiterfassungCore

final class ModelDecodingTests: XCTestCase {
    private let decoder: JSONDecoder = {
        let d = JSONDecoder()
        d.keyDecodingStrategy = .convertFromSnakeCase
        return d
    }()

    // MARK: - User

    func testUserDecoding() throws {
        let json = """
        {
          "id": "u1",
          "email": "alice@example.com",
          "first_name": "Alice",
          "last_name": "Smith",
          "employee_number": "E001",
          "phone": null,
          "photo_url": null,
          "manager_id": null,
          "is_active": true,
          "roles": ["EMPLOYEE"],
          "permissions": ["time.view.own"]
        }
        """.data(using: .utf8)!
        let user = try decoder.decode(User.self, from: json)
        XCTAssertEqual(user.id, "u1")
        XCTAssertEqual(user.email, "alice@example.com")
        XCTAssertEqual(user.firstName, "Alice")
        XCTAssertEqual(user.lastName, "Smith")
        XCTAssertEqual(user.employeeNumber, "E001")
        XCTAssertTrue(user.isActive)
        XCTAssertEqual(user.roles, ["EMPLOYEE"])
        XCTAssertEqual(user.fullName, "Alice Smith")
    }

    func testUserOptionalFieldsNil() throws {
        let json = """
        {
          "id": "u2",
          "email": "bob@example.com",
          "first_name": "Bob",
          "last_name": "Jones",
          "employee_number": null,
          "phone": null,
          "photo_url": null,
          "manager_id": null,
          "is_active": false,
          "roles": [],
          "permissions": []
        }
        """.data(using: .utf8)!
        let user = try decoder.decode(User.self, from: json)
        XCTAssertNil(user.employeeNumber)
        XCTAssertNil(user.phone)
        XCTAssertNil(user.photoUrl)
        XCTAssertNil(user.managerId)
        XCTAssertFalse(user.isActive)
    }

    // MARK: - LoginResponse

    func testLoginResponseDecoding() throws {
        let json = """
        {
          "access_token": "tok123",
          "refresh_token": "ref456",
          "expires_in": 900,
          "user": {
            "id": "u1",
            "email": "a@b.com",
            "first_name": "A",
            "last_name": "B",
            "employee_number": null,
            "phone": null,
            "photo_url": null,
            "manager_id": null,
            "is_active": true,
            "roles": [],
            "permissions": []
          }
        }
        """.data(using: .utf8)!
        let response = try decoder.decode(LoginResponse.self, from: json)
        XCTAssertEqual(response.accessToken, "tok123")
        XCTAssertEqual(response.refreshToken, "ref456")
        XCTAssertEqual(response.expiresIn, 900)
        XCTAssertEqual(response.user.id, "u1")
    }

    // MARK: - TrackingStatusResponse

    func testTrackingStatusClockedOut() throws {
        let json = """
        {
          "status": "CLOCKED_OUT",
          "clocked_in_since": null,
          "break_started_at": null,
          "elapsed_work_minutes": 0,
          "elapsed_break_minutes": 0,
          "today_work_minutes": 240,
          "today_break_minutes": 30
        }
        """.data(using: .utf8)!
        let status = try decoder.decode(TrackingStatusResponse.self, from: json)
        XCTAssertEqual(status.status, "CLOCKED_OUT")
        XCTAssertNil(status.clockedInSince)
        XCTAssertNil(status.breakStartedAt)
        XCTAssertEqual(status.todayWorkMinutes, 240)
    }

    func testTrackingStatusClockedIn() throws {
        let json = """
        {
          "status": "CLOCKED_IN",
          "clocked_in_since": "2026-03-01T08:00:00Z",
          "break_started_at": null,
          "elapsed_work_minutes": 120,
          "elapsed_break_minutes": 0,
          "today_work_minutes": 120,
          "today_break_minutes": 0
        }
        """.data(using: .utf8)!
        let status = try decoder.decode(TrackingStatusResponse.self, from: json)
        XCTAssertEqual(status.status, "CLOCKED_IN")
        XCTAssertEqual(status.clockedInSince, "2026-03-01T08:00:00Z")
        XCTAssertEqual(status.elapsedWorkMinutes, 120)
    }

    // MARK: - VacationBalance

    func testVacationBalanceDecoding() throws {
        let json = """
        {
          "id": "vb1",
          "user_id": "u1",
          "year": 2026,
          "total_days": 30.0,
          "used_days": 5.0,
          "carried_over_days": 2.0,
          "remaining_days": 27.0
        }
        """.data(using: .utf8)!
        let balance = try decoder.decode(VacationBalance.self, from: json)
        XCTAssertEqual(balance.id, "vb1")
        XCTAssertEqual(balance.year, 2026)
        XCTAssertEqual(balance.totalDays, 30.0, accuracy: 0.001)
        XCTAssertEqual(balance.remainingDays, 27.0, accuracy: 0.001)
    }

    // MARK: - VacationRequest

    func testVacationRequestDecoding() throws {
        let json = """
        {
          "id": "vr1",
          "user_id": "u1",
          "start_date": "2026-04-01",
          "end_date": "2026-04-05",
          "is_half_day_start": false,
          "is_half_day_end": false,
          "total_days": 5.0,
          "status": "PENDING",
          "approved_by": null,
          "rejection_reason": null,
          "notes": "Easter vacation",
          "created_at": "2026-03-01T10:00:00Z"
        }
        """.data(using: .utf8)!
        let request = try decoder.decode(VacationRequest.self, from: json)
        XCTAssertEqual(request.id, "vr1")
        XCTAssertEqual(request.status, .pending)
        XCTAssertEqual(request.totalDays, 5.0, accuracy: 0.001)
        XCTAssertEqual(request.notes, "Easter vacation")
    }

    func testVacationRequestAllStatuses() throws {
        let statuses: [(String, VacationRequest.Status)] = [
            ("PENDING", .pending),
            ("APPROVED", .approved),
            ("REJECTED", .rejected),
            ("CANCELLED", .cancelled),
        ]
        for (raw, expected) in statuses {
            let json = """
            {
              "id": "x",
              "user_id": "u1",
              "start_date": "2026-01-01",
              "end_date": "2026-01-02",
              "is_half_day_start": false,
              "is_half_day_end": false,
              "total_days": 1.0,
              "status": "\(raw)",
              "approved_by": null,
              "rejection_reason": null,
              "notes": null,
              "created_at": "2026-01-01T00:00:00Z"
            }
            """.data(using: .utf8)!
            let request = try decoder.decode(VacationRequest.self, from: json)
            XCTAssertEqual(request.status, expected, "Status \(raw) should decode to \(expected)")
        }
    }

    // MARK: - PageResponse

    func testPageResponseDecoding() throws {
        let json = """
        {
          "content": [],
          "total_elements": 0,
          "total_pages": 0,
          "page_number": 0,
          "page_size": 20
        }
        """.data(using: .utf8)!
        let page = try decoder.decode(PageResponse<VacationRequest>.self, from: json)
        XCTAssertEqual(page.content.count, 0)
        XCTAssertEqual(page.totalElements, 0)
        XCTAssertEqual(page.pageSize, 20)
    }
}
