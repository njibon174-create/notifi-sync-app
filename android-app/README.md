# Notification Sync - Android App

Android client for the Cross-Device Notification Sync system. This is **Stage 3** only: authentication + device registration.

## Included in this stage

- Email/password **Login** and **Sign Up** using Supabase Auth
- Secure session storage with **EncryptedSharedPreferences**
- Automatic session restore / refresh on app launch
- Device registration against the live Supabase `devices` table
- Placeholder home screen showing the signed-in email and registered device
- No SMS capture and no notification listener yet — that is Stage 4

## Tech stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: lightweight MVVM (single `AppViewModel`)
- **Networking**: direct Supabase REST calls via OkHttp
- **Min SDK**: 26

## Project layout

```text
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/notifsync/app/
│   │   │   ├── AppContainer.kt
│   │   │   ├── AppViewModel.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── NotificationSyncApplication.kt
│   │   │   ├── data/
│   │   │   └── ui/
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── gradlew / gradlew.bat
└── local.properties.example
```

## Configuration

Create your own `local.properties` from the example file:

```bash
cp local.properties.example local.properties
```

Then set your Supabase values:

```properties
SUPABASE_URL=https://epqakuroqjtrhhcyweoa.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

These are injected into `BuildConfig` at build time from `app/build.gradle.kts`.

### Notes

- Do **not** commit your real `local.properties`
- The app only needs the **anon key** on device
- Never ship the `service role` key in the Android app

## How to run locally

1. Open the `android-app` folder in Android Studio
2. Let Gradle sync finish
3. Add your `local.properties`
4. Run on an emulator or Android phone

### Command line build

```bash
cd android-app
./gradlew assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Flow implemented

### 1) Login / Sign Up screen
- Email + password fields
- Toggle between Login and Sign Up
- Uses Supabase Auth
- Stores the session securely with EncryptedSharedPreferences

### 2) Device registration screen
- Prompts for a friendly device name
- Pre-fills the device model from `Build.MODEL`
- Posts to the live `devices` table
- Stores the returned `device_id` locally so Stage 4 can tag uploads

### 3) Home screen
- Shows the logged-in email
- Shows the registered device name
- Includes Logout

### 4) Session handling
- If a valid session exists and a device is already registered → Home
- If a valid session exists but device is not registered → Device Registration
- If there is no valid session → Login
- Logout clears auth tokens but keeps the local device registration

## GitHub Actions release build

The repo includes `.github/workflows/android-release.yml`.

I chose **tagged releases** (`v*`) instead of every push to `main`, because:
- it keeps the Releases page clean and versioned
- the APK matches a stable tag, not every intermediate commit
- it is easier to download and install a known build

The workflow:
- builds `assembleRelease`
- uploads the APK as a workflow artifact
- publishes the APK to the GitHub Release assets for tagged builds

## Backend used

- **Supabase Project URL**: `https://epqakuroqjtrhhcyweoa.supabase.co`
- **Auth endpoint**: `https://epqakuroqjtrhhcyweoa.supabase.co/auth/v1`
- **REST endpoint**: `https://epqakuroqjtrhhcyweoa.supabase.co/rest/v1`

## Endpoints used by the app

- `POST /auth/v1/signup`
- `POST /auth/v1/token?grant_type=password`
- `POST /auth/v1/token?grant_type=refresh_token`
- `POST /rest/v1/devices`

## Not implemented yet

- SMS capture
- Notification listener access
- Background sync to `notifications`
- Sync status UI
