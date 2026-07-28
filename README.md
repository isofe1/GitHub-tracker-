# 🚀 Release Hub - GitHub Release & APK Manager

**Release Hub** is a modern, clean, and lightweight Android application designed to track GitHub repositories, monitor software releases, and download APKs or assets directly to your device with one tap.

---

## ✨ Features

- 🔖 **GitHub Repository Tracking**: Track your favorite open-source projects using GitHub URLs (e.g., `github.com/owner/repo`).
- 🔔 **Automatic Release Alerts**: Monitor background release updates with native Android notifications via WorkManager.
- ⚡ **Direct File & APK Downloads**: Integrated multi-threaded download engine with real-time speed, progress indicators, and instant APK installation / sharing.
- 📂 **Download History & Management**: Search, filter by file extension (APKs, ZIPs, Docs), and manage downloaded assets stored on your local device.
- ⚙️ **Dual Download Mode**: Flexible downloading with either the high-performance In-App Download Engine or Android's System Download Manager.
- 🎨 **Modern & Minimalist UI**: Built using Jetpack Compose and Material Design 3 with edge-to-edge layout support.

---

## 🛠 Tech Stack & Architecture

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Database**: Room Database for persistent local storage of tracked repositories and download logs
- **Networking**: OkHttp & Retrofit for GitHub REST API integration
- **Background Tasks**: WorkManager for recurring background release checks
- **Concurrency**: Kotlin Coroutines & StateFlow

---

## 📱 Screenshots & User Experience

Release Hub features three core intuitive tabs:
1. **Projects Tab**: View and track repositories, check latest releases, and download release assets instantly.
2. **Direct Link Tab**: Paste any direct GitHub asset link or download URL for direct downloading.
3. **Downloads Tab**: Manage ongoing downloads, open/install downloaded APKs, share files, and view transfer speeds.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 24+ (Android 7.0 minimum support)

### Building the Project
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/release-hub.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and run the `:app` configuration on an Android device or emulator.

---

## 🔒 Permissions & Security

- `INTERNET` & `ACCESS_NETWORK_STATE`: For fetching repository data and downloading release assets.
- `POST_NOTIFICATIONS`: For alerting users when new releases are detected on Android 13+.
- `REQUEST_INSTALL_PACKAGES`: Allows seamless one-tap installation of downloaded APK files.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
