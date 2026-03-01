import Foundation
import Combine

@MainActor
class ServerSettingsViewModel: ObservableObject {
    @Published var serverUrl: String
    @Published var isManaged: Bool
    @Published var saveSuccess: Bool = false

    private let configManager: ServerConfigManager

    init(configManager: ServerConfigManager = .shared) {
        self.configManager = configManager
        self.serverUrl = configManager.effectiveServerUrl
        self.isManaged = configManager.isManaged
    }

    func saveServerUrl(_ url: String) {
        configManager.saveServerUrl(url)
        serverUrl = url
        saveSuccess = true
    }
}
