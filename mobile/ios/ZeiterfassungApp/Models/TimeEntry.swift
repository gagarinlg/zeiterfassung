import Foundation

struct TimeEntry: Codable, Identifiable {
    let id: String
    let userId: String
    let entryType: EntryType
    let timestamp: Date
    let source: Source
    let notes: String?
    let isModified: Bool

    enum EntryType: String, Codable {
        case clockIn = "CLOCK_IN"
        case clockOut = "CLOCK_OUT"
        case breakStart = "BREAK_START"
        case breakEnd = "BREAK_END"
    }

    enum Source: String, Codable {
        case web = "WEB"
        case mobile = "MOBILE"
        case terminal = "TERMINAL"
    }
}
