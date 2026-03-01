import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = DashboardViewModel()

    private func formatMinutes(_ minutes: Int) -> String {
        let h = minutes / 60
        let m = minutes % 60
        return String(format: "%dh %02dm", h, m)
    }

    var body: some View {
        NavigationView {
            Group {
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            if let error = viewModel.errorMessage {
                                Text(error)
                                    .foregroundColor(.red)
                                    .font(.caption)
                            }

                            // Status summary row
                            HStack(spacing: 12) {
                                SummaryCard(
                                    title: "dashboard_today_hours".localized,
                                    value: formatMinutes(viewModel.trackingStatus?.todayWorkMinutes ?? 0)
                                )
                                if let balance = viewModel.vacationBalance {
                                    SummaryCard(
                                        title: "dashboard_vacation_remaining".localized,
                                        value: String(format: "%.1f d", balance.remainingDays)
                                    )
                                }
                            }
                            .padding(.horizontal)

                            // Current status card
                            if let status = viewModel.trackingStatus {
                                VStack(alignment: .leading, spacing: 8) {
                                    let statusLabel: String = {
                                        switch status.status {
                                        case "CLOCKED_IN": return "time_tracking_clock_in".localized
                                        case "ON_BREAK": return "time_tracking_break_start".localized
                                        default: return "time_tracking_clock_out".localized
                                        }
                                    }()
                                    let statusColor: Color = {
                                        switch status.status {
                                        case "CLOCKED_IN": return .green
                                        case "ON_BREAK": return .orange
                                        default: return .red
                                        }
                                    }()
                                    HStack {
                                        Circle()
                                            .fill(statusColor)
                                            .frame(width: 10, height: 10)
                                        Text(statusLabel)
                                            .font(.headline)
                                            .foregroundColor(statusColor)
                                    }
                                    if status.status != "CLOCKED_OUT" {
                                        Text(formatMinutes(status.elapsedWorkMinutes))
                                            .font(.title2)
                                            .fontWeight(.semibold)
                                    }
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding()
                                .background(Color(.secondarySystemBackground))
                                .cornerRadius(12)
                                .padding(.horizontal)
                            }
                        }
                        .padding(.top)
                    }
                }
            }
            .navigationTitle("nav_dashboard".localized)
            .task {
                if let userId = authViewModel.currentUserId {
                    await viewModel.load(userId: userId)
                }
            }
        }
    }
}

private struct SummaryCard: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.title3)
                .fontWeight(.semibold)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}
