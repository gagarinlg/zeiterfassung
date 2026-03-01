import SwiftUI

struct TimeTrackingView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = TimeTrackingViewModel()

    private func formatMinutes(_ minutes: Int) -> String {
        let hours = minutes / 60
        let mins = minutes % 60
        return String(format: "%dh %02dm", hours, mins)
    }

    var body: some View {
        NavigationView {
            Group {
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    VStack(spacing: 20) {
                        if let error = viewModel.errorMessage {
                            Text(error).foregroundColor(.red).font(.caption)
                        }

                        if let error = viewModel.actionError {
                            Text(error).foregroundColor(.red).font(.caption)
                                .onAppear {
                                    Task {
                                        try? await Task.sleep(nanoseconds: 3_000_000_000)
                                        viewModel.clearActionError()
                                    }
                                }
                        }

                        let status = viewModel.trackingStatus?.status ?? "CLOCKED_OUT"
                        let statusColor: Color = {
                            switch status {
                            case "CLOCKED_IN": return .green
                            case "ON_BREAK": return .orange
                            default: return .red
                            }
                        }()
                        let statusLabel: String = {
                            switch status {
                            case "CLOCKED_IN": return "time_tracking_clock_in".localized
                            case "ON_BREAK": return "time_tracking_break_start".localized
                            default: return "time_tracking_clock_out".localized
                            }
                        }()

                        // Status pill
                        Text(statusLabel)
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(statusColor)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(statusColor.opacity(0.12))
                            .clipShape(Capsule())

                        if let trackingStatus = viewModel.trackingStatus, trackingStatus.status != "CLOCKED_OUT" {
                            Text(formatMinutes(trackingStatus.elapsedWorkMinutes))
                                .font(.system(size: 48, weight: .thin, design: .monospaced))
                        }

                        Spacer()

                        // Action buttons
                        VStack(spacing: 12) {
                            switch status {
                            case "CLOCKED_OUT":
                                ActionButton(
                                    label: "time_tracking_clock_in".localized,
                                    color: .green,
                                    isLoading: viewModel.isActionLoading
                                ) {
                                    Task {
                                        if let userId = authViewModel.currentUserId {
                                            await viewModel.clockIn(userId: userId)
                                        }
                                    }
                                }
                            case "CLOCKED_IN":
                                ActionButton(
                                    label: "time_tracking_break_start".localized,
                                    color: .orange,
                                    isLoading: viewModel.isActionLoading
                                ) {
                                    Task {
                                        if let userId = authViewModel.currentUserId {
                                            await viewModel.startBreak(userId: userId)
                                        }
                                    }
                                }
                                ActionButton(
                                    label: "time_tracking_clock_out".localized,
                                    color: .red,
                                    isLoading: viewModel.isActionLoading
                                ) {
                                    Task {
                                        if let userId = authViewModel.currentUserId {
                                            await viewModel.clockOut(userId: userId)
                                        }
                                    }
                                }
                            case "ON_BREAK":
                                ActionButton(
                                    label: "time_tracking_break_end".localized,
                                    color: .blue,
                                    isLoading: viewModel.isActionLoading
                                ) {
                                    Task {
                                        if let userId = authViewModel.currentUserId {
                                            await viewModel.endBreak(userId: userId)
                                        }
                                    }
                                }
                                ActionButton(
                                    label: "time_tracking_clock_out".localized,
                                    color: .red,
                                    isLoading: viewModel.isActionLoading
                                ) {
                                    Task {
                                        if let userId = authViewModel.currentUserId {
                                            await viewModel.clockOut(userId: userId)
                                        }
                                    }
                                }
                            default:
                                EmptyView()
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 32)

                        // Today totals
                        if let trackingStatus = viewModel.trackingStatus {
                            HStack {
                                Label("\("time_tracking_today_work".localized): \(formatMinutes(trackingStatus.todayWorkMinutes))", systemImage: "briefcase")
                                Spacer()
                                Label("\("time_tracking_today_break".localized): \(formatMinutes(trackingStatus.todayBreakMinutes))", systemImage: "cup.and.heat.waves")
                            }
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.horizontal)
                        }
                    }
                    .padding(.top)
                }
            }
            .navigationTitle("nav_time_tracking".localized)
            .task {
                if let userId = authViewModel.currentUserId {
                    await viewModel.loadStatus(userId: userId)
                }
            }
        }
    }
}

private struct ActionButton: View {
    let label: String
    let color: Color
    let isLoading: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            if isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
            } else {
                Text(label)
                    .frame(maxWidth: .infinity)
            }
        }
        .buttonStyle(.borderedProminent)
        .tint(color)
        .disabled(isLoading)
    }
}
