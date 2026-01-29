# 🔧 Building and Running Mind Mint

This guide will help you set up your development environment and build the **Mind Mint** application from source.

---

## ✅ Prerequisites

Before you begin, ensure you have the following installed:

- **[Android Studio](https://developer.android.com/studio)** (Ladybug or newer recommended)
- **Java Development Kit (JDK)**: Version 17 or 21 (Android Studio usually bundles this).
- **Git**: To clone the repository.
- **Android Device or Emulator**: Running Android 9 (Pie) or higher.

---

## 🚀 Build Instructions

### Option 1: Using Android Studio (Recommended)

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/gtxPrime/Mind-Mint.git
    ```
2.  **Open Project**:
    - Launch Android Studio -> `Open`.
    - Navigate to the cloned folder and select it.
3.  **Sync Gradle**:
    - Android Studio should automatically start syncing.
    - If not, click `File` -> `Sync Project with Gradle Files`.
    - _Wait for the sync to complete (this may take a few minutes first time)._
4.  **Run**:
    - Connect your device or start an emulator.
    - Click the green **Run** ▶️ button in the toolbar.

### Option 2: Command Line

You can build the APK directly using the Gradle Wrapper included in the project.

**Windows:**

```powershell
./gradlew assembleDebug
```

**Mac/Linux:**

```bash
./gradlew assembleDebug
```

The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🐛 Troubleshooting

### "SDK location not found"

- Create a file named `local.properties` in the root directory.
- Add the path to your SDK: `sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk` (Windows) or `sdk.dir=/Users/YourName/Library/Android/sdk` (Mac).

### "Could not resolve dependencies"

- Ensure you have an active internet connection.
- Try running `File` -> `Invalidate Caches / Restart`.

### "Permission Denied" (Linux/Mac)

- Run the following to make the wrapper executable:
  ```bash
  chmod +x gradlew
  ```

---

## 🧪 Testing

To run the automated tests:

**Unit Tests:**

```bash
./gradlew test
```

**Instrumented (UI) Tests:**

```bash
./gradlew connectedAndroidTest
```
