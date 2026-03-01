import Foundation

/// Manages server URL configuration with priority: MDM > UserDefaults > default.
class ServerConfigManager {
    static let shared = ServerConfigManager()

    private let serverUrlKey = "server_url"
    private let defaultUrl = "https://zeiterfassung.example.com/api"

    private init() {}

    /// Returns the effective server URL: MDM managed config > user setting > default.
    var effectiveServerUrl: String {
        if let managedUrl = managedServerUrl, !managedUrl.isEmpty {
            return managedUrl
        }
        if let savedUrl = userServerUrl, !savedUrl.isEmpty {
            return savedUrl
        }
        return defaultUrl
    }

    /// Whether the server URL is managed via MDM and cannot be changed by the user.
    var isManaged: Bool {
        if let managedUrl = managedServerUrl, !managedUrl.isEmpty {
            return true
        }
        return false
    }

    /// Server URL from MDM managed app configuration.
    var managedServerUrl: String? {
        let managedConfig = UserDefaults.standard.dictionary(forKey: "com.apple.configuration.managed")
        return managedConfig?["server_url"] as? String
    }

    /// Server URL saved by the user.
    var userServerUrl: String? {
        get { UserDefaults.standard.string(forKey: serverUrlKey) }
        set { UserDefaults.standard.set(newValue, forKey: serverUrlKey) }
    }

    /// Save a user-provided server URL.
    func saveServerUrl(_ url: String) {
        userServerUrl = url
    }

    /// Clear the user-provided server URL.
    func clearServerUrl() {
        UserDefaults.standard.removeObject(forKey: serverUrlKey)
    }
}
