import XCTest
@testable import ZeiterfassungCore

final class DateFormatterExtensionsTests: XCTestCase {
    // MARK: - iso8601Full

    func testIso8601FullParsesKnownDate() {
        let string = "2026-03-01T10:30:00.000+00:00"
        let date = DateFormatter.iso8601Full.date(from: string)
        XCTAssertNotNil(date)
    }

    func testIso8601FullFormatsAndParseRoundTrip() {
        let original = Date(timeIntervalSince1970: 1_740_000_000)
        let formatted = DateFormatter.iso8601Full.string(from: original)
        let parsed = DateFormatter.iso8601Full.date(from: formatted)
        XCTAssertNotNil(parsed)
        // Allow up to 1 second tolerance due to sub-second rounding
        XCTAssertEqual(original.timeIntervalSince1970, parsed!.timeIntervalSince1970, accuracy: 1.0)
    }

    func testIso8601FullRejectsInvalidString() {
        let date = DateFormatter.iso8601Full.date(from: "not-a-date")
        XCTAssertNil(date)
    }

    // MARK: - dateOnly

    func testDateOnlyFormatsCorrectly() {
        // Use a fixed date: 2026-03-01
        var components = DateComponents()
        components.year = 2026
        components.month = 3
        components.day = 1
        let date = Calendar.current.date(from: components)!
        let formatted = DateFormatter.dateOnly.string(from: date)
        XCTAssertEqual(formatted, "2026-03-01")
    }

    func testDateOnlyParsesCorrectly() {
        let date = DateFormatter.dateOnly.date(from: "2026-03-15")
        XCTAssertNotNil(date)
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month, .day], from: date!)
        XCTAssertEqual(components.year, 2026)
        XCTAssertEqual(components.month, 3)
        XCTAssertEqual(components.day, 15)
    }

    // MARK: - timeOnly

    func testTimeOnlyFormatsHoursAndMinutes() {
        // Create a fixed time using DateComponents so the test is timezone-independent
        var components = DateComponents()
        components.year = 2026
        components.month = 1
        components.day = 1
        components.hour = 9
        components.minute = 30
        components.second = 0
        components.timeZone = TimeZone(secondsFromGMT: 0)
        let date = Calendar(identifier: .gregorian).date(from: components)!
        // Force formatter to use UTC so the test is reproducible on any machine
        let formatter = DateFormatter.timeOnly
        let savedTimeZone = formatter.timeZone
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        let formatted = formatter.string(from: date)
        formatter.timeZone = savedTimeZone
        // Pattern: exactly HH:mm
        let regex = try! NSRegularExpression(pattern: #"^\d{2}:\d{2}$"#)
        let matches = regex.matches(in: formatted, range: NSRange(formatted.startIndex..., in: formatted))
        XCTAssertFalse(matches.isEmpty, "timeOnly should produce HH:mm format, got '\(formatted)'")
        XCTAssertEqual(formatted, "09:30")
    }
}

// MARK: - String extensions

final class StringExtensionsTests: XCTestCase {
    func testLocalizedReturnsNonEmptyString() {
        // Without a bundle, NSLocalizedString returns the key itself
        let key = "some_key"
        let localized = key.localized
        XCTAssertFalse(localized.isEmpty)
    }

    func testLocalizedWithArgumentsReturnsNonEmptyString() {
        // Without a translation bundle, NSLocalizedString returns the key itself.
        // The test verifies the helper doesn't crash and returns a result.
        let key = "hello %@"
        let result = key.localized(with: "World")
        XCTAssertFalse(result.isEmpty)
    }
}
