import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Text("app_name".localized)
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.accentColor)

                Text("app_tagline".localized)
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Spacer().frame(height: 16)

                if let error = authViewModel.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }

                VStack(spacing: 16) {
                    TextField("auth_email".localized, text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .textFieldStyle(.roundedBorder)

                    SecureField("auth_password".localized, text: $password)
                        .textFieldStyle(.roundedBorder)
                }

                Button(action: {
                    Task {
                        await authViewModel.login(email: email, password: password)
                    }
                }) {
                    if authViewModel.isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    } else {
                        Text("auth_login_button".localized)
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(authViewModel.isLoading)
            }
            .padding(32)
        }
    }
}
