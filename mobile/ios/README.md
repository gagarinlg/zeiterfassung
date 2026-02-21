# Zeiterfassung iOS App

## Requirements

- Xcode 15+
- iOS 16+ deployment target
- Swift 5.9+

## Project Structure

```
ZeiterfassungApp/
├── App/
│   ├── ZeiterfassungApp.swift       # App entry point (@main)
│   └── ContentView.swift            # Root content view
├── Views/
│   ├── LoginView.swift
│   ├── DashboardView.swift
│   ├── TimeTrackingView.swift
│   └── VacationView.swift
├── ViewModels/
│   ├── AuthViewModel.swift
│   ├── DashboardViewModel.swift
│   ├── TimeTrackingViewModel.swift
│   └── VacationViewModel.swift
├── Models/
│   ├── User.swift
│   ├── TimeEntry.swift
│   └── VacationRequest.swift
├── Services/
│   ├── APIClient.swift
│   ├── AuthService.swift
│   └── TimeService.swift
├── Utils/
│   └── DateFormatter+Extensions.swift
└── Resources/
    ├── en.lproj/Localizable.strings
    └── de.lproj/Localizable.strings
```

## Setup

1. Open `ZeiterfassungApp.xcodeproj` in Xcode
2. Set the API base URL in `Services/APIClient.swift` or use environment configuration
3. Run on simulator or device

## Localization

The app supports German (primary) and English. To add a new language:
1. Add a new `.lproj` directory in Resources
2. Copy `en.lproj/Localizable.strings` and translate all strings
3. Add the language in Xcode project settings
