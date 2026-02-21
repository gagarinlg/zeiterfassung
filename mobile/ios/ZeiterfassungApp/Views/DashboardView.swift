import SwiftUI

struct DashboardView: View {
    var body: some View {
        NavigationView {
            Text("dashboard_title".localized)
                .navigationTitle("nav_dashboard".localized)
        }
    }
}
