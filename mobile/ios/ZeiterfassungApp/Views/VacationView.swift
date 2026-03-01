import SwiftUI

struct VacationView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = VacationViewModel()

    var body: some View {
        NavigationView {
            Group {
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        if let error = viewModel.errorMessage {
                            Section {
                                Text(error).foregroundColor(.red).font(.caption)
                            }
                        }

                        if let balance = viewModel.balance {
                            Section("vacation_remaining".localized) {
                                HStack {
                                    BalanceRow(label: "vacation_total".localized, value: String(format: "%.1f d", balance.totalDays))
                                    Divider()
                                    BalanceRow(label: "vacation_used".localized, value: String(format: "%.1f d", balance.usedDays))
                                    Divider()
                                    BalanceRow(
                                        label: "vacation_remaining_label".localized,
                                        value: String(format: "%.1f d", balance.remainingDays),
                                        valueColor: balance.remainingDays < 5 ? .red : .primary
                                    )
                                }
                                .frame(maxWidth: .infinity)
                            }
                        }

                        if !viewModel.requests.isEmpty {
                            Section("vacation_request".localized) {
                                ForEach(viewModel.requests) { request in
                                    VacationRequestRow(request: request)
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("nav_vacation".localized)
            .task {
                if let userId = authViewModel.currentUserId {
                    await viewModel.load(userId: userId)
                }
            }
        }
    }
}

private struct BalanceRow: View {
    let label: String
    let value: String
    var valueColor: Color = .primary

    var body: some View {
        VStack(spacing: 4) {
            Text(label).font(.caption).foregroundColor(.secondary)
            Text(value).font(.headline).foregroundColor(valueColor)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct VacationRequestRow: View {
    let request: VacationRequest

    private var statusColor: Color {
        switch request.status {
        case .approved: return .green
        case .rejected: return .red
        case .cancelled: return .gray
        default: return .orange
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("\(request.startDate) – \(request.endDate)")
                    .font(.subheadline)
                Spacer()
                Text(request.status.rawValue)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(statusColor)
            }
            Text(String(format: "%.1f %@", request.totalDays, "common_days".localized))
                .font(.caption)
                .foregroundColor(.secondary)
            if let reason = request.rejectionReason, !reason.isEmpty {
                Text(reason)
                    .font(.caption2)
                    .foregroundColor(.red)
            }
        }
    }
}

