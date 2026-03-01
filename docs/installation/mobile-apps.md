# Mobile App Installation & Deployment

This guide covers building, configuring, and distributing the Zeiterfassung mobile apps for Android and iOS.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Android](#android)
  - [Building from Source](#android-building-from-source)
  - [Configuring the Server URL](#android-configuring-the-server-url)
  - [Signing](#android-signing)
  - [Distribution](#android-distribution)
  - [MDM Provisioning](#android-mdm-provisioning)
- [iOS](#ios)
  - [Building from Source](#ios-building-from-source)
  - [Configuring the Server URL](#ios-configuring-the-server-url)
  - [Signing](#ios-signing)
  - [Distribution](#ios-distribution)
  - [MDM Provisioning](#ios-mdm-provisioning)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Android

| Requirement       | Minimum Version |
|-------------------|-----------------|
| Android Studio    | Hedgehog (2023.1) or later |
| JDK               | 17+             |
| Android SDK       | API 34 (compileSdk) |
| Min SDK           | API 26 (Android 8.0) |
| Gradle            | 8.x (bundled wrapper) |

### iOS

| Requirement       | Minimum Version |
|-------------------|-----------------|
| macOS             | Ventura 13+ (Sonoma 14+ recommended) |
| Xcode             | 15.0+           |
| Swift             | 5.9+            |
| iOS deployment target | 16.0+       |
| CocoaPods / SPM   | Swift Package Manager (included) |

---

## Android

<a id="android-building-from-source"></a>

### Building from Source

#### Debug Build

```bash
cd mobile/android
./gradlew assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

#### Release Build

A release build requires a signing keystore (see [Signing](#android-signing)):

```bash
cd mobile/android
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=/path/to/keystore.jks \
  -Pandroid.injected.signing.store.password=YOUR_STORE_PASSWORD \
  -Pandroid.injected.signing.key.alias=YOUR_KEY_ALIAS \
  -Pandroid.injected.signing.key.password=YOUR_KEY_PASSWORD
```

The signed APK is written to:

```
app/build/outputs/apk/release/app-release.apk
```

To produce an Android App Bundle (AAB) for Google Play:

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

<a id="android-configuring-the-server-url"></a>

### Configuring the Server URL

The app connects to the Zeiterfassung backend API. The server URL is resolved with the following priority:

1. **MDM managed configuration** (highest priority — cannot be changed by the user)
2. **User setting** (configured in the app's Settings screen)
3. **Default** (`https://zeiterfassung.example.com/api/`)

#### Changing the Default URL at Build Time

Edit the default in `mobile/android/app/src/main/kotlin/com/zeiterfassung/app/di/NetworkModule.kt`:

```kotlin
private const val BASE_URL_DEFAULT = "https://your-server.example.com/api/"
```

Also update the restriction default in `mobile/android/app/src/main/res/xml/app_restrictions.xml`:

```xml
<restriction
    android:key="server_url"
    ...
    android:defaultValue="https://your-server.example.com/api/" />
```

#### Changing the URL at Runtime (User Setting)

Users can change the server URL in the app under **Settings → Server URL**, unless the URL is locked by an MDM profile (see [MDM Provisioning](#android-mdm-provisioning)).

<a id="android-signing"></a>

### Signing

#### Creating a Keystore

```bash
keytool -genkeypair \
  -v \
  -keystore zeiterfassung-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias zeiterfassung \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Zeiterfassung, OU=Mobile, O=YourCompany, L=Berlin, ST=Berlin, C=DE"
```

> **Security:** Never commit the keystore or passwords to version control. Store them in a secrets manager or CI/CD secret variables.

#### Configuring Signing in `build.gradle.kts`

For automated builds, you can configure signing in the Gradle file or pass the values via command-line properties as shown above.

<a id="android-distribution"></a>

### Distribution

#### Direct APK Installation

1. Transfer the APK to the device (USB, file share, download link).
2. On the device, enable **Settings → Security → Unknown Sources** (or per-app install permission on Android 8+).
3. Open the APK file and tap **Install**.

#### Google Play Store

1. Build a signed AAB (`./gradlew bundleRelease`).
2. Create a Google Play Developer account at [play.google.com/console](https://play.google.com/console).
3. Create a new app, upload the AAB, fill in the store listing.
4. Submit for review.

For internal testing, use the **Internal Testing** track to distribute to a small group without review.

#### Firebase App Distribution (Internal Testing)

```bash
./gradlew assembleRelease appDistributionUploadRelease
```

Requires the Firebase App Distribution Gradle plugin configured in `build.gradle.kts`.

<a id="android-mdm-provisioning"></a>

### MDM Provisioning

The app supports **Android Enterprise managed configurations** via `app_restrictions.xml`. When deployed through an MDM solution, administrators can pre-configure the server URL so employees do not need to enter it manually.

See [Mobile Provisioning Guide](mobile-provisioning.md) for detailed MDM configuration examples.

**Configuration key:**

| Key          | Type   | Description                        | Default |
|--------------|--------|------------------------------------|---------|
| `server_url` | String | Backend API base URL               | `https://zeiterfassung.example.com/api/` |

---

## iOS

<a id="ios-building-from-source"></a>

### Building from Source

The iOS app uses Swift Package Manager and is structured as a Swift package at `mobile/ios/`.

#### Opening in Xcode

```bash
cd mobile/ios
open Package.swift
```

Xcode will resolve dependencies automatically via Swift Package Manager.

#### Building from Command Line

```bash
cd mobile/ios
xcodebuild -scheme ZeiterfassungApp \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  build
```

#### Archive for Distribution

```bash
xcodebuild -scheme ZeiterfassungApp \
  -destination 'generic/platform=iOS' \
  -archivePath build/Zeiterfassung.xcarchive \
  archive

xcodebuild -exportArchive \
  -archivePath build/Zeiterfassung.xcarchive \
  -exportOptionsPlist ExportOptions.plist \
  -exportPath build/export
```

<a id="ios-configuring-the-server-url"></a>

### Configuring the Server URL

The `ServerConfigManager` resolves the server URL with the following priority:

1. **MDM managed app configuration** (highest priority — UI shows URL as read-only)
2. **User setting** (configured in the app's Server Settings screen)
3. **Default** (`https://zeiterfassung.example.com/api`)

The implementation is in `mobile/ios/ZeiterfassungApp/Services/ServerConfigManager.swift`:

```swift
var effectiveServerUrl: String {
    if let managedUrl = managedServerUrl, !managedUrl.isEmpty {
        return managedUrl
    }
    if let savedUrl = userServerUrl, !savedUrl.isEmpty {
        return savedUrl
    }
    return defaultUrl
}
```

#### Changing the Default URL

Edit the `defaultUrl` constant in `ServerConfigManager.swift`:

```swift
private let defaultUrl = "https://your-server.example.com/api"
```

#### MDM-Managed URL

When a `server_url` key is present in the `com.apple.configuration.managed` dictionary, it takes precedence and the user cannot override it. The `isManaged` property returns `true`, and the Settings UI should display the URL as read-only.

<a id="ios-signing"></a>

### Signing

#### Requirements

- **Apple Developer Account** (Individual or Organization)
- **Development Certificate** — for debug builds and testing
- **Distribution Certificate** — for App Store and TestFlight
- **Provisioning Profile** — ties the certificate to specific app ID and devices

#### Setting Up Signing in Xcode

1. Open the project in Xcode.
2. Select the target → **Signing & Capabilities** tab.
3. Check **Automatically manage signing**.
4. Select your team.

#### Manual Signing (CI/CD)

For CI/CD pipelines, export the certificate and provisioning profile:

```bash
# Install certificate from base64-encoded .p12
echo "$CERTIFICATE_BASE64" | base64 --decode > cert.p12
security import cert.p12 -k ~/Library/Keychains/login.keychain-db \
  -P "$CERTIFICATE_PASSWORD" -T /usr/bin/codesign

# Install provisioning profile
cp profile.mobileprovision ~/Library/MobileDevice/Provisioning\ Profiles/
```

<a id="ios-distribution"></a>

### Distribution

#### TestFlight (Recommended for Internal Testing)

1. Archive the app in Xcode (**Product → Archive**).
2. In the Organizer, click **Distribute App → TestFlight & App Store**.
3. Upload to App Store Connect.
4. Add internal/external testers in App Store Connect.

#### App Store

1. Complete the App Store listing in App Store Connect.
2. Upload a production-signed build via Xcode or `altool`.
3. Submit for App Review.

#### Ad Hoc Distribution

For distributing to specific devices without the App Store:

1. Register device UDIDs in the Apple Developer portal.
2. Create an Ad Hoc provisioning profile.
3. Archive and export with the Ad Hoc profile.
4. Distribute the `.ipa` file via a download link or MDM.

<a id="ios-mdm-provisioning"></a>

### MDM Provisioning

The app reads managed app configuration from the `com.apple.configuration.managed` UserDefaults key. When deployed through an MDM solution, administrators can push the server URL directly to devices.

See [Mobile Provisioning Guide](mobile-provisioning.md) for detailed MDM configuration examples.

**Configuration key:**

| Key          | Type   | Description                        | Default |
|--------------|--------|------------------------------------|---------|
| `server_url` | String | Backend API base URL               | `https://zeiterfassung.example.com/api` |

---

## Troubleshooting

### Android

| Problem | Solution |
|---------|----------|
| `SDK location not found` | Create `local.properties` in `mobile/android/` with `sdk.dir=/path/to/Android/Sdk` |
| Build fails with JDK errors | Ensure JDK 17+ is installed and `JAVA_HOME` is set correctly |
| App cannot reach server | Verify the server URL ends with `/api/` (trailing slash). Check that the server's CORS configuration allows the app's requests |
| `CLEARTEXT communication not permitted` | The default config requires HTTPS. For local development with HTTP, add `android:usesCleartextTraffic="true"` to `AndroidManifest.xml` (debug only) |
| APK install blocked | Enable **Install unknown apps** for the file manager or browser on the device |
| ProGuard/R8 strips classes | Check `proguard-rules.pro` for missing keep rules for Retrofit models |

### iOS

| Problem | Solution |
|---------|----------|
| `No such module` errors | Close Xcode, delete `.build/` folder, reopen and let SPM resolve |
| Signing errors | Ensure your Apple Developer account is active and the provisioning profile matches the bundle ID |
| Simulator not available | Download the required iOS simulator runtime in Xcode → Settings → Platforms |
| Network requests fail on simulator | The simulator uses the Mac's network. Ensure the backend is reachable from the Mac |
| MDM config not applied | On real devices, MDM config requires the app to be installed via MDM. Simulator testing requires manual UserDefaults injection |
| SwiftLint warnings | Run `swiftlint` from `mobile/ios/` — the `.swiftlint.yml` configuration is at the project root |

### General

| Problem | Solution |
|---------|----------|
| Login fails with 401 | Verify the server URL is correct and the backend is running. Check that the user account exists and is active |
| SSL certificate errors | Ensure the server has a valid SSL certificate. For self-signed certificates, configure certificate pinning or trust the CA on the device |
| App crashes on launch | Check the device logs (`adb logcat` for Android, Xcode Console for iOS) for stack traces |
