import SwiftUI

struct ServerSettingsView: View {
    @StateObject private var viewModel = ServerSettingsViewModel()
    @State private var editableUrl: String = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("server_settings_title".localized)) {
                    if viewModel.isManaged {
                        Text("server_settings_managed_info".localized)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    TextField("server_settings_url_placeholder".localized, text: $editableUrl)
                        .keyboardType(.URL)
                        .autocapitalization(.none)
                        .autocorrectionDisabled()
                        .disabled(viewModel.isManaged)

                    if !viewModel.isManaged {
                        Button(action: {
                            viewModel.saveServerUrl(editableUrl)
                        }) {
                            Text("server_settings_save".localized)
                        }
                    }

                    if viewModel.saveSuccess {
                        Text("server_settings_restart_hint".localized)
                            .font(.caption)
                            .foregroundColor(.green)
                    }
                }

                Section(header: Text("server_settings_info_header".localized)) {
                    Text("server_settings_info_body".localized)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("server_settings_title".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Text("common_close".localized)
                    }
                }
            }
            .onAppear {
                editableUrl = viewModel.serverUrl
            }
        }
    }
}
