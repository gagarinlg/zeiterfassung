# Mobile App Provisioning & MDM Configuration

This guide covers enterprise provisioning of the Zeiterfassung mobile apps via Mobile Device Management (MDM) solutions. MDM allows administrators to pre-configure the server URL so employees can use the app immediately without manual setup.

---

## Table of Contents

- [Overview](#overview)
- [Configuration Priority](#configuration-priority)
- [Configuration Keys](#configuration-keys)
- [Android Enterprise Managed Configurations](#android-enterprise-managed-configurations)
  - [Schema Reference (app\_restrictions.xml)](#schema-reference)
  - [Google Workspace (Google Endpoint Management)](#google-workspace)
  - [VMware Workspace ONE (AirWatch)](#vmware-workspace-one-android)
  - [Microsoft Intune](#microsoft-intune-android)
- [iOS Managed App Configuration](#ios-managed-app-configuration)
  - [Configuration Mechanism](#ios-configuration-mechanism)
  - [Jamf Pro](#jamf-pro)
  - [Microsoft Intune](#microsoft-intune-ios)
  - [VMware Workspace ONE](#vmware-workspace-one-ios)
- [Testing Provisioning Locally](#testing-provisioning-locally)
  - [Android](#testing-android)
  - [iOS](#testing-ios)
- [Troubleshooting](#troubleshooting)

---

## Overview

Both the Android and iOS apps support receiving configuration from an MDM server. This is the recommended approach for enterprise deployments because:

- Employees do not need to know or type the server URL.
- The URL cannot be accidentally changed by the user.
- Configuration can be updated remotely without app reinstallation.
- It integrates with existing device management workflows.

## Configuration Priority

Both platforms use the same priority order when resolving the server URL:

```
┌─────────────────────┐
│  MDM Configuration  │  ← Highest priority (read-only for user)
├─────────────────────┤
│   User Setting      │  ← Can be changed in app Settings
├─────────────────────┤
│   Default URL       │  ← Compiled into the app
└─────────────────────┘
```

When an MDM configuration is present, the server URL is shown as **read-only** in the app's settings screen, and the user cannot override it.

## Configuration Keys

| Key          | Type   | Required | Description | Example |
|--------------|--------|----------|-------------|---------|
| `server_url` | String | Yes      | Full base URL of the Zeiterfassung backend API, including the `/api` path | `https://zeiterfassung.example.com/api` |

> **Note:** The URL should point to the API base path. Do not include a trailing slash for iOS. The Android app expects a trailing slash (`/api/`). Consult the platform-specific sections below.

---

## Android Enterprise Managed Configurations

Android Enterprise uses **managed configurations** (formerly app restrictions) to push settings to managed apps. The schema is defined in the app's `app_restrictions.xml` file.

<a id="schema-reference"></a>

### Schema Reference (`app_restrictions.xml`)

The file is located at `mobile/android/app/src/main/res/xml/app_restrictions.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<restrictions xmlns:android="http://schemas.android.com/apk/res/android">
    <restriction
        android:key="server_url"
        android:title="@string/restriction_server_url_title"
        android:description="@string/restriction_server_url_description"
        android:restrictionType="string"
        android:defaultValue="https://zeiterfassung.example.com/api/" />
</restrictions>
```

**Fields:**

| Attribute | Value |
|-----------|-------|
| `key` | `server_url` |
| `restrictionType` | `string` |
| `defaultValue` | `https://zeiterfassung.example.com/api/` |

The app reads this configuration using `RestrictionsManager` on Android. When a value is provided by the MDM, it takes precedence over any user-configured setting.

<a id="google-workspace"></a>

### Google Workspace (Google Endpoint Management)

1. Open [Google Admin Console](https://admin.google.com) → **Devices → Mobile & endpoints → Apps → Android apps**.
2. Search for and select the Zeiterfassung app (or add it as a private app).
3. Click **Managed configurations**.
4. Set the `server_url` field:

```
server_url = https://zeiterfassung.yourcompany.com/api/
```

5. Assign the configuration to the target organizational units (OUs).

<a id="vmware-workspace-one-android"></a>

### VMware Workspace ONE (AirWatch)

1. Navigate to **Apps & Books → Applications → Native → Internal**.
2. Select the Zeiterfassung app → **Assignment** tab.
3. Under **Application Configuration**, add:

| Key          | Value Type | Value |
|--------------|-----------|-------|
| `server_url` | String    | `https://zeiterfassung.yourcompany.com/api/` |

4. Save and publish the assignment.

Example XML payload for Workspace ONE API:

```xml
<ManagedApplicationConfiguration>
  <ConfigurationItem>
    <Key>server_url</Key>
    <ValueType>String</ValueType>
    <Value>https://zeiterfassung.yourcompany.com/api/</Value>
  </ConfigurationItem>
</ManagedApplicationConfiguration>
```

<a id="microsoft-intune-android"></a>

### Microsoft Intune (Android)

1. Open [Microsoft Intune admin center](https://intune.microsoft.com) → **Apps → Android apps**.
2. Select the Zeiterfassung app → **Properties → App configuration policies**.
3. Create a new policy:
   - **Platform:** Android Enterprise
   - **Profile type:** Managed devices
4. Under **Configuration settings**, use the **Configuration designer**:

| Configuration Key | Value Type | Value |
|-------------------|-----------|-------|
| `server_url`      | String    | `https://zeiterfassung.yourcompany.com/api/` |

Alternatively, use JSON format:

```json
{
  "kind": "androidenterprise#managedConfiguration",
  "managedProperty": [
    {
      "key": "server_url",
      "valueString": "https://zeiterfassung.yourcompany.com/api/"
    }
  ]
}
```

5. Assign the policy to the target device groups.

---

## iOS Managed App Configuration

<a id="ios-configuration-mechanism"></a>

### Configuration Mechanism

iOS uses **Managed App Configuration** to push key-value pairs to apps. The configuration is delivered as a property list dictionary via the `com.apple.configuration.managed` UserDefaults key.

The app reads this configuration using:

```swift
let managedConfig = UserDefaults.standard.dictionary(
    forKey: "com.apple.configuration.managed"
)
let serverUrl = managedConfig?["server_url"] as? String
```

When a `server_url` value is present in the managed configuration, the `ServerConfigManager.isManaged` property returns `true`, and the Settings UI displays the URL as read-only.

### Configuration Dictionary

```xml
<dict>
    <key>server_url</key>
    <string>https://zeiterfassung.yourcompany.com/api</string>
</dict>
```

> **Note:** For iOS, do **not** include a trailing slash in the URL.

<a id="jamf-pro"></a>

### Jamf Pro

1. Navigate to **Devices → Mobile Device Apps** (or **Computers → macOS Apps**).
2. Select the Zeiterfassung app.
3. Go to the **App Configuration** tab.
4. Enter the managed app configuration payload:

```xml
<dict>
    <key>server_url</key>
    <string>https://zeiterfassung.yourcompany.com/api</string>
</dict>
```

5. Scope the app to the target devices or groups.
6. Save.

**Full Jamf configuration profile example:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>server_url</key>
    <string>https://zeiterfassung.yourcompany.com/api</string>
</dict>
</plist>
```

<a id="microsoft-intune-ios"></a>

### Microsoft Intune (iOS)

1. Open [Microsoft Intune admin center](https://intune.microsoft.com) → **Apps → App configuration policies**.
2. Create a new policy:
   - **Platform:** iOS/iPadOS
   - **Associated app:** Zeiterfassung
3. Under **Configuration settings**, select **Use configuration designer**:

| Configuration Key | Value Type | Value |
|-------------------|-----------|-------|
| `server_url`      | String    | `https://zeiterfassung.yourcompany.com/api` |

Alternatively, enter the XML property list directly:

```xml
<dict>
    <key>server_url</key>
    <string>https://zeiterfassung.yourcompany.com/api</string>
</dict>
```

4. Assign the policy to the target device groups.

<a id="vmware-workspace-one-ios"></a>

### VMware Workspace ONE (iOS)

1. Navigate to **Apps & Books → Applications → Native**.
2. Select the Zeiterfassung app → **Assignment** tab.
3. Under **Managed App Configuration**, enter:

```xml
<dict>
    <key>server_url</key>
    <string>https://zeiterfassung.yourcompany.com/api</string>
</dict>
```

4. Save and publish.

---

## Testing Provisioning Locally

<a id="testing-android"></a>

### Android

#### Using Android Debug Bridge (ADB)

You can simulate managed configuration using the **Test DPC** app (Device Policy Controller) from Google:

1. Install [Test DPC](https://play.google.com/store/apps/details?id=com.afwsamples.testdpc) on your test device or emulator.
2. Set up a managed profile using Test DPC.
3. Install the Zeiterfassung app within the managed profile.
4. In Test DPC, navigate to **Manage app restrictions**.
5. Select the Zeiterfassung app.
6. Set `server_url` to your test server URL.

#### Using Android Studio

1. Build and run the app on an emulator.
2. Use the **App Inspection** tool to verify that `RestrictionsManager` returns the expected values.

#### Programmatic Verification

Add temporary logging in debug builds to verify restriction values:

```kotlin
val restrictionsManager = getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
val appRestrictions = restrictionsManager.applicationRestrictions
val serverUrl = appRestrictions.getString("server_url")
Log.d("MDM", "Managed server_url: $serverUrl")
```

<a id="testing-ios"></a>

### iOS

#### Using Simulator with UserDefaults

MDM configuration is delivered via `UserDefaults`. You can simulate this on the iOS Simulator:

```bash
# Set managed configuration via command line
xcrun simctl spawn booted defaults write com.zeiterfassung.app \
  com.apple.configuration.managed \
  -dict server_url "https://test-server.example.com/api"
```

Then launch the app in the Simulator. The `ServerConfigManager` will read the managed configuration and use the provided URL.

#### Clearing Test Configuration

```bash
xcrun simctl spawn booted defaults delete com.zeiterfassung.app \
  com.apple.configuration.managed
```

#### Using Xcode

You can set launch arguments or environment variables in the Xcode scheme to test different configurations:

1. Edit scheme → **Run → Arguments**.
2. Add an argument: `-com.apple.configuration.managed '{"server_url": "https://test.example.com/api"}'`

> **Note:** On real devices, managed configuration is only available when the app is installed via MDM. The Simulator workaround above is for development testing only.

---

## Troubleshooting

| Problem | Platform | Solution |
|---------|----------|----------|
| MDM config not applied | Both | Ensure the app is installed through the MDM (not sideloaded). MDM configuration is only delivered to managed apps |
| URL changes not reflected | Both | The app reads the configuration at startup. Force-quit and relaunch the app after pushing new MDM configuration |
| User can still edit URL | Both | Verify the MDM policy is correctly assigned. Check that the `server_url` key matches exactly (case-sensitive) |
| Connection fails after provisioning | Both | Verify the URL is correct and reachable from the device network. Check HTTPS certificate validity |
| Android: Restrictions not available | Android | Ensure the device is enrolled in Android Enterprise (not legacy device admin). Managed configurations require a work profile or fully managed device |
| iOS: `com.apple.configuration.managed` is nil | iOS | On real devices, the app must be installed via MDM. On Simulator, use the `defaults write` command above |
| Trailing slash mismatch | Both | Android expects a trailing slash (`/api/`), iOS does not (`/api`). Configure accordingly per platform |
