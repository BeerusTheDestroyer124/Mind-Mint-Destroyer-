# 🔑 Key Components Deep Dive

This document provides a detailed look at the most critical files in the **Mind Mint** codebase. Understanding these components is key to modifying or extending the app.

---

## 🧠 Core Services

These services run in the background and power the core functionality.

### 1. `AppUsageAccessibilityService.java`

- **Location**: `Services/`
- **Role**: **The Guardian**. This service monitors user interaction to enforce focus.
- **Key Methods**:
  - `onAccessibilityEvent(AccessibilityEvent event)`:
    - **Event Filtering**: Checks `event.getEventType()` to distinguish between window changes (app launches) and scrolling.
    - **Blocking Logic**: Compares `event.getPackageName()` against `custom_blocked_apps_set`. If matched, calls `startActivity(BlockingOverlayDisplayActivity)`.
    - **Doom Scroll Detection**: For specific apps (Instagram, YouTube, Snap), it tracks `TYPE_VIEW_SCROLLED` events. It applies a debounce (e.g., 1000ms for Insta, 2500ms for YT) to estimate "wasted time" and increments a scroll counter in SharedPreferences.
    - **Global Time Limit**: Aggregates usage time in `appTotalWastedTimeToday` map. If usage > `pref_block_after_wasted_time_hours`, it triggers a block.

### 2. `FocusService.java`

- **Location**: `Services/`
- **Role**: **The Focus Engine**.
- **Key Features**:
  - **Room Persistence**: Uses `FocusDao` to save `FocusStateEntity`. This allows the timer to _resume exactly where it left off_ even if the app process is killed by Android.
  - **Pomodoro Logic**: Manages `isPomodoroEnabled` state. Automatically transitions between "Focus" and "Break" states using `transitionPomodoroState()`.
  - **Coin Economy**: On `stopTimer()`, calculates focus duration. If the session completed naturally, it awards Mint Crystals via `MintCrystals.addCoins()`.
  - **Foreground Service**: Runs with `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (on Android 14+) to ensure priority.

---

## 📊 Data Layer & Managers

The app uses a strict separation between data storage (Room/Prefs) and logic (Managers).

### 3. Room Database (`MindMintRoomDatabase`)

The source of truth for complex data.

- **`FocusSessionEntity`**: Stores history of every focus session (start/end time, duration, topic).
- **`FocusDailyStatEntity`**: Aggregates seconds focused per day (used for graphs).
- **`HabitEntity`**: User's habits and configurations.
- **`DailyStatsEntity`**: Usage stats for blocked apps.

### 4. `MintCrystals.java`

- **Role**: **Economy System**.
- **Logic**:
  - Uses SharedPreferences to store a secure integer balance.
  - `addCoins(int amount)` / `subtractCoins(int amount)`: Atomic operations to modify balance.
  - Used to purchase items or unlock features (future scope).

### 5. `TaskManager.java` & `HabitManager.java`

- **Role**: **Logic Controllers**.
- **Pattern**: Singleton.
- **Function**: Encapsulates all CRUD operations. For example, `HabitManager.checkInHabit(id)` handles streak updates, checking if it was already done today, and firing completion events.

---

## 📱 UI Components

### 6. `BlockingOverlayDisplayActivity.java`

- **Role**: **The Shield**.
- **Mechanism**:
  - Launched with `FLAG_ACTIVITY_NEW_TASK` | `FLAG_ACTIVITY_CLEAR_TOP` to aggressively take over the screen.
  - Overrides `onBackPressed()` to redirect the user to their Home Screen (`Intent.CATEGORY_HOME`), preventing them from returning to the blocked app.

### 7. `FocusMode.java`

- **Role**: **Immersive Experience**.
- **Visuals**:
  - `LottieAnimationView`: Displays the "Crystal" growing state based on `FocusService` progress (`crystalRevealFraction`).
  - `NebulaStarfieldView`: A custom SurfaceView that draws a moving starfield for ambience.

---

## 🔔 Receivers

### 8. `MidnightResetReceiver.java`

- **Trigger**: `AlarmManager` (approx 12:00 AM).
- **Duties**:
  - Calls `HabitManager.resetDailyHabits()` to uncheck daily habits.
  - Resets `appTotalWastedTimeToday` map for fresh daily limits.
  - Updates Streak counters for habits that were missed.
