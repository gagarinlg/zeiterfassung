import SwiftUI

struct MainTabView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var showServerSettings = false

    var body: some View {
        NavigationStack {
            TabView {
                DashboardView()
                    .tabItem {
                        Label("nav_dashboard".localized, systemImage: "chart.bar")
                    }

                TimeTrackingView()
                    .tabItem {
                        Label("nav_time_tracking".localized, systemImage: "clock")
                    }

                VacationView()
                    .tabItem {
                        Label("nav_vacation".localized, systemImage: "calendar")
                    }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(action: { showServerSettings = true }) {
                            Label("nav_server_settings".localized, systemImage: "server.rack")
                        }
                        Button(role: .destructive, action: { authViewModel.logout() }) {
                            Label("auth_logout_button".localized, systemImage: "rectangle.portrait.and.arrow.right")
                        }
                    } label: {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .sheet(isPresented: $showServerSettings) {
                ServerSettingsView()
            }
        }
    }
}
