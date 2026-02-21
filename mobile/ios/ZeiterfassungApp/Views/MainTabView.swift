import SwiftUI

struct MainTabView: View {
    var body: some View {
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
    }
}
