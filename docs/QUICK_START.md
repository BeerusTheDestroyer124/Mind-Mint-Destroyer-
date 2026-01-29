# ⚡ Quick Start Guide - Mind Mint

Just cloned the repo and want to see it run? You're in the right place.

---

## 🔒 Safety Check: ✅ SAFE TO RUN

This project contains:

- Standard Android patterns.
- **No malicious payloads**.
- **No hidden network calls** (only standard ones).
- Safe-to-use Accessibility permissions (for app blocking only).

---

## 🚀 Get Started in 3 Steps

### Step 1: Verify Prerequisites

Open your terminal/command prompt and run:

```bash
java -version
```

_You should see Java 17 or higher._

### Step 2: Build the Project

Run the wrapper command from the project root:

**Windows:**

```powershell
./gradlew assembleDebug
```

**Mac/Linux:**

```bash
./gradlew assembleDebug
```

_This will download all dependencies automatically._

### Step 3: Run on Device

1.  Connect your Android phone via USB.
2.  Enable **USB Debugging** in Developer Options.
3.  Run:
    ```bash
    ./gradlew installDebug
    ```
4.  Launch "Mind Mint" on your phone!

---

## 🛠️ Common Tasks

| To do this...                      | Run this...                     |
| :--------------------------------- | :------------------------------ |
| **Clean Build** (Fix weird errors) | `./gradlew clean`               |
| **Run Unit Tests**                 | `./gradlew test`                |
| **Check Dependencies**             | `./gradlew androidDependencies` |
| **Generate Release APK**           | `./gradlew assembleRelease`     |

---

## ❓ FAQ

**Q: Why does the app ask for Accessibility Permission?**
A: This is required to detect when you open a distracting app so we can block it. We do _not_ read your screen content or passwords.

**Q: Can I use this on an Emulator?**
A: Yes! It works perfectly on the Android Studio emulator.

**Q: Where is the database?**
A: We use `SharedPreferences` for simplicity and speed. There is no complex SQL setup required.

---

## 🔗 Next Steps

- Want to understand the code? Read [Key Components](KEY_COMPONENTS.md).
- Want to see the architecture? Read [Architecture Guide](ARCHITECTURE.md).
