package com.gxdevs.mindmint.Utils;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.gxdevs.mindmint.MindMintApp;

/**
 * Centralised helper for logging Firebase Analytics events and Crashlytics metadata.
 *
 * Usage — call from any Activity, Fragment, or Service:
 * <pre>
 *   // Focus session started
 *   FirebaseAnalyticsHelper.logFocusSessionStarted("pomodoro", 25);
 *
 *   // Focus session completed
 *   FirebaseAnalyticsHelper.logFocusSessionCompleted("open_ended", 47);
 *
 *   // Lock In started
 *   FirebaseAnalyticsHelper.logLockInStarted(60);
 *
 *   // Task completed
 *   FirebaseAnalyticsHelper.logTaskCompleted();
 *
 *   // Habit checked off
 *   FirebaseAnalyticsHelper.logHabitChecked("exercise");
 *
 *   // Screen viewed
 *   FirebaseAnalyticsHelper.logScreenView("FocusMode", "FocusMode");
 *
 *   // Non-fatal exception (so Crashlytics records it without a crash)
 *   FirebaseAnalyticsHelper.logNonFatal(e, "tag");
 * </pre>
 */
public class FirebaseAnalyticsHelper {

    private static final String TAG = "AnalyticsHelper";

    // ── Custom event names ───────────────────────────────────────────────────
    public static final String EVENT_FOCUS_START      = "focus_session_started";
    public static final String EVENT_FOCUS_COMPLETE   = "focus_session_completed";
    public static final String EVENT_FOCUS_PAUSED     = "focus_session_paused";
    public static final String EVENT_FOCUS_STOPPED    = "focus_session_stopped";
    public static final String EVENT_LOCK_IN_START    = "lock_in_started";
    public static final String EVENT_LOCK_IN_COMPLETE = "lock_in_completed";
    public static final String EVENT_TASK_CREATED     = "task_created";
    public static final String EVENT_TASK_COMPLETED   = "task_completed";
    public static final String EVENT_HABIT_CHECKED    = "habit_checked";
    public static final String EVENT_HABIT_CREATED    = "habit_created";
    public static final String EVENT_SETTINGS_LOCK_ON = "settings_lock_enabled";
    public static final String EVENT_FCM_RECEIVED     = "fcm_notification_received";

    // ── Parameter names ──────────────────────────────────────────────────────
    public static final String PARAM_SESSION_TYPE  = "session_type";  // pomodoro | open_ended | task_linked | locked_in
    public static final String PARAM_DURATION_MIN  = "duration_minutes";
    public static final String PARAM_HABIT_NAME    = "habit_name";
    public static final String PARAM_NOTIF_TYPE    = "notification_type";

    // ─────────────────────────────────────────────────────────────────────────
    //  Focus events
    // ─────────────────────────────────────────────────────────────────────────

    public static void logFocusSessionStarted(String sessionType, long durationMinutes) {
        Bundle b = new Bundle();
        b.putString(PARAM_SESSION_TYPE, sessionType);
        b.putLong(PARAM_DURATION_MIN, durationMinutes);
        log(EVENT_FOCUS_START, b);
    }

    public static void logFocusSessionCompleted(String sessionType, long actualMinutes) {
        Bundle b = new Bundle();
        b.putString(PARAM_SESSION_TYPE, sessionType);
        b.putLong(PARAM_DURATION_MIN, actualMinutes);
        log(EVENT_FOCUS_COMPLETE, b);
    }

    public static void logFocusSessionPaused(String sessionType) {
        Bundle b = new Bundle();
        b.putString(PARAM_SESSION_TYPE, sessionType);
        log(EVENT_FOCUS_PAUSED, b);
    }

    public static void logFocusSessionStopped(String sessionType) {
        Bundle b = new Bundle();
        b.putString(PARAM_SESSION_TYPE, sessionType);
        log(EVENT_FOCUS_STOPPED, b);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lock In events
    // ─────────────────────────────────────────────────────────────────────────

    public static void logLockInStarted(long durationMinutes) {
        Bundle b = new Bundle();
        b.putLong(PARAM_DURATION_MIN, durationMinutes);
        log(EVENT_LOCK_IN_START, b);
    }

    public static void logLockInCompleted(long actualMinutes) {
        Bundle b = new Bundle();
        b.putLong(PARAM_DURATION_MIN, actualMinutes);
        log(EVENT_LOCK_IN_COMPLETE, b);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Task events
    // ─────────────────────────────────────────────────────────────────────────

    public static void logTaskCreated() {
        log(EVENT_TASK_CREATED, null);
    }

    public static void logTaskCompleted() {
        log(EVENT_TASK_COMPLETED, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Habit events
    // ─────────────────────────────────────────────────────────────────────────

    public static void logHabitChecked(String habitName) {
        Bundle b = new Bundle();
        b.putString(PARAM_HABIT_NAME, habitName);
        log(EVENT_HABIT_CHECKED, b);
    }

    public static void logHabitCreated() {
        log(EVENT_HABIT_CREATED, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FCM events
    // ─────────────────────────────────────────────────────────────────────────

    public static void logFcmReceived(String notificationType) {
        Bundle b = new Bundle();
        b.putString(PARAM_NOTIF_TYPE, notificationType != null ? notificationType : "general");
        log(EVENT_FCM_RECEIVED, b);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Screen tracking
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Logs a screen_view event (mirrors FirebaseAnalytics.logEvent(SCREEN_VIEW)).
     *
     * @param screenName  e.g. "FocusMode", "HomeActivity"
     * @param screenClass e.g. "FocusMode", "HomeActivity"
     */
    public static void logScreenView(String screenName, String screenClass) {
        Bundle b = new Bundle();
        b.putString(FirebaseAnalytics.Param.SCREEN_NAME,  screenName);
        b.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
        log(FirebaseAnalytics.Event.SCREEN_VIEW, b);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Crashlytics helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Records a non-fatal exception in Crashlytics without crashing.
     * Great for caught exceptions you still want to track.
     */
    public static void logNonFatal(Throwable t, String context) {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            if (context != null) crashlytics.setCustomKey("error_context", context);
            crashlytics.recordException(t);
        } catch (Exception ignored) {
            Log.e(TAG, "Crashlytics recording failed", t);
        }
    }

    /**
     * Attaches a key–value pair to every subsequent Crashlytics report in this session.
     * Useful for tagging the user's current state (e.g. screen, session type).
     */
    public static void setCrashlyticsKey(String key, String value) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value != null ? value : "null");
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal
    // ─────────────────────────────────────────────────────────────────────────

    private static void log(String event, Bundle params) {
        try {
            FirebaseAnalytics analytics = MindMintApp.getAnalytics();
            if (analytics != null) {
                analytics.logEvent(event, params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Analytics log failed for event: " + event, e);
        }
    }
}
