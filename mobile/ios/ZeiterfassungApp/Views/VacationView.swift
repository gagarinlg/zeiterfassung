import SwiftUI

struct VacationView: View {
    var body: some View {
        NavigationView {
            Text("vacation_title".localized)
                .navigationTitle("nav_vacation".localized)
        }
    }
}
