package com.gxdevs.mindmint.Services;

import static com.gxdevs.mindmint.Utils.Utils.isKeepAlive;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.util.Pair;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.gxdevs.mindmint.Activities.BlockingOverlayDisplayActivity;
import com.gxdevs.mindmint.Common.IntentActions;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Receivers.MidnightResetReceiver;
import com.gxdevs.mindmint.Receivers.NotificationDismissBroadcastReceiver;
import com.gxdevs.mindmint.Receivers.ServiceResumeReceiver;
import com.gxdevs.mindmint.Utils.AdultDomainListManager;
import com.gxdevs.mindmint.Utils.AlarmUtils;
import com.gxdevs.mindmint.Utils.BlockedSitesManager;
import com.gxdevs.mindmint.Utils.MintCrystals;
import com.gxdevs.mindmint.Utils.Utils;
import com.gxdevs.mindmint.Utils.WarningUtils;

import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppUsageAccessibilityService extends AccessibilityService {

    private static final String TAG = "AppUsageAccessibilityService";
    private static final String PREFS_NAME = "AppData";
    // Reuse the Guard service's notification so only one notification is shown
    private static final String FGS_CHANNEL_ID = "mindmint_guard_channel";
    private static final int FGS_NOTIFICATION_ID = 42;

    // --- Constants for Custom Blocking (Focus Mode) ---
    public static final String PREF_CUSTOM_BLOCKED_APPS = "custom_blocked_apps_set";
    public static final String PREF_BLOCKING_POPUP_DURATION_SEC = "blocking_popup_duration_seconds";
    public static final String BLOCKING_OVERLAY_ACTIVITY_CLASS_NAME = "com.gxdevs.mindmint.Activities.BlockingOverlayDisplayActivity";
    public static final String EXTRA_BLOCKED_APP_NAME = "extra_blocked_app_name";
    public static final String EXTRA_BLOCKED_PACKAGE_NAME = "extra_blocked_package_name";
    public static final String EXTRA_IS_REMINDER_ONLY = "extra_is_reminder_only";
    public static final String EXTRA_IS_FOCUS = "extra_is_focus";

    public static final String PREF_REMIND_DOOM_SCROLLING_ENABLED = "pref_remind_doom_scrolling_enabled";
    public static final String PREF_REMIND_DOOM_SCROLLING_MINUTES = "pref_remind_doom_scrolling_minutes";
    public static final int DEFAULT_REMIND_DOOM_SCROLLING_MINUTES = 10;
    public static final String PREF_BLOCK_AFTER_WASTED_TIME_ENABLED = "pref_block_after_wasted_time_enabled";
    public static final String PREF_BLOCK_AFTER_WASTED_TIME_HOURS = "pref_block_after_wasted_time_hours";
    public static final float DEFAULT_BLOCK_AFTER_WASTED_TIME_HOURS = 1.0f;
    public static final String PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED = "pref_block_browsers_doom_enabled";
    public static final String PREF_BLOCK_ADULT_SITES_ENABLED = "pref_block_adult_sites_enabled";

    public static final String PREF_APP_VIEW_ACCUMULATED_TIME_MS_PREFIX = "app_view_accumulated_time_ms_";
    public static final String PREF_LAST_VIEW_REMINDER_TIMESTAMP_PREFIX = "last_view_reminder_timestamp_";
    public static final String PREF_LAST_REMINDER_TIMESTAMP_APP_TAG_PREFIX = "last_reminder_timestamp_app_tag_";
    public static final String PREF_REMINDER_VIEW_ACCUMULATED_TIME_CYCLE_MS_APP_TAG_PREFIX = "reminder_view_accumulated_time_cycle_ms_app_tag_";

    // Action for the overlay to tell this service to close the current app
    public static final String ACTION_PERFORM_GLOBAL_HOME_FROM_OVERLAY = "com.gxdevs.mindmint.action.PERFORM_GLOBAL_HOME_FROM_OVERLAY";
    public static final String ACTION_PERFORM_GLOBAL_BACK_FROM_OVERLAY = "com.gxdevs.mindmint.action.PERFORM_GLOBAL_BACK_FROM_OVERLAY";

    // Broadcast Action for internal state refresh at midnight
    public static final String ACTION_REFRESH_DAILY_STATE_INTERNAL = "com.gxdevs.mindmint.action.REFRESH_DAILY_STATE_INTERNAL";
    public static final String ACTION_UPDATE_KEEP_ALIVE = "com.gxdevs.mindmint.action.UPDATE_KEEP_ALIVE";

    // Preference keys for service notification (not used in original)
    private SharedPreferences sharedPreferences;
    private SharedPreferences prefs;
    private long startTimeMillis = 0L;
    private String currentPackage = null;
    private BroadcastReceiver packageReceiver;
    private BroadcastReceiver overlayCommandReceiver;
    private BroadcastReceiver midnightStateRefreshReceiver;
    private BroadcastReceiver pauseServiceReceiver;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    // --- Variables for Back Pressing ---
    private boolean isYtHomeSwitchOn = false;
    private boolean isInstaHomeSwitchOn = false;
    private boolean isSnapHomeSwitchOn = false;

    // --- Variables for Custom Blocking (Focus Mode) ---
    private Set<String> customBlockedApps = new HashSet<>();

    // --- Variables for Reminders ---
    private boolean remindDoomScrollingEnabled;
    private int remindDoomScrollingMinutes;
    private final Map<String, Long> lastReminderTimestampForAppTag = new HashMap<>();
    private final Map<String, Long> appTagToTimeLeftForNextSessionMs = new HashMap<>();
    private final Map<String, Long> appTagToOriginalDelayMs = new HashMap<>();
    private final Map<String, Long> appTagToLastPostTimeMs = new HashMap<>();
    private final Map<String, ShowReminderRunnable> activeRunnables = new HashMap<>();
    private String currentAppTagForReminderViewTracking = null;
    private Handler reminderHandler;
    private final Map<String, Long> lastScrollEventTimestamp = new HashMap<>();

    // Cache for browser package checks
    private final Map<String, Boolean> browserCheckCache = new HashMap<>();

    // saveReminderViewAccumulatedTime method.
    private final Map<String, Long> reminderViewSessionStartTimeMs = new HashMap<>();
    private final Map<String, Long> reminderViewAccumulatedTimeCurrentCycleMs = new HashMap<>();

    // --- Variables for Blocking (Wasted Time) & General App Usage ---
    private boolean blockAfterWastedTimeEnabled;
    private float blockAfterWastedTimeHours;
    private final Map<String, Long> appTotalWastedTimeToday = new HashMap<>(); // For global wasted time blocking & UI
    private boolean blockBrowsersDoomEnabled = false;
    private boolean blockAdultSitesEnabled = false;

    // Cache the most recently checked adult domain and its result
    private String lastAdultCheckedDomain = null;
    private boolean lastAdultCheckedBlocked = false;

    // --- Variables for old View Focus system (potentially needs review/cleanup)
    private final Map<String, Long> currentViewFocusSessionStartTimeMap = new HashMap<>();
    private final Map<String, Long> appViewFocusAccumulatedTimeTodayMap = new HashMap<>();
    private String currentViewIdPackage = null;

    // --- PeaceCoins Reminder Ignore Tracking ---
    private static final String PREF_REMINDER_IGNORED_COUNT_PREFIX = "reminder_ignored_count_";

    // --- Global action throttling ---
    private long lastActionTime = 0L;
    public static boolean serviceStatus = false;
    private BroadcastReceiver screenReceiver;
    private boolean isScreenReceiverRegistered = false;
    public static final String RESTORE_NOTIFICATION = "restore_notification";
    private BroadcastReceiver notificationRestoreReceiver;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        BlockedSitesManager.ensureSetsExist(getApplicationContext());

        notificationRestoreReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if (RESTORE_NOTIFICATION.equals(intent.getAction())
                            || ACTION_UPDATE_KEEP_ALIVE.equals(intent.getAction())) {
                        if (isKeepAlive(context)) {
                            updateAddonsState(); // Force update
                        } else {
                            // Explicitly stop if toggle turned off and broadcast received
                            stopForeground(true);
                            if (isScreenReceiverRegistered) {
                                unregisterScreenReceiver();
                            }
                            AlarmUtils.cancelAlarm(AppUsageAccessibilityService.this);
                            WarningUtils.remove(AppUsageAccessibilityService.this);
                        }
                    }
                }
            }
        };

        IntentFilter notiFilter = new IntentFilter();
        notiFilter.addAction(RESTORE_NOTIFICATION);
        notiFilter.addAction(ACTION_UPDATE_KEEP_ALIVE);
        ContextCompat.registerReceiver(this, notificationRestoreReceiver, notiFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);

        isYtHomeSwitchOn = false;
        isInstaHomeSwitchOn = false;
        isSnapHomeSwitchOn = false;

        pauseServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long pauseDuration = intent.getLongExtra("pause_duration", 0);
                if (pauseDuration > 0) {
                    long resumeTime = System.currentTimeMillis() + pauseDuration;
                    sharedPreferences.edit()
                            .putBoolean("isServicePaused", true)
                            .putLong("resumeTime", resumeTime)
                            .apply();
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Intent resumeIntent = new Intent(context, ServiceResumeReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, resumeIntent,
                            PendingIntent.FLAG_IMMUTABLE);
                    try {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, resumeTime, pendingIntent);
                    } catch (SecurityException e) {
                        Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    sharedPreferences.edit()
                            .putBoolean("isServicePaused", false)
                            .putLong("resumeTime", 0)
                            .apply();
                }
            }
        };
        IntentFilter pauseFilter = new IntentFilter(IntentActions.getActionPauseService(this));
        ContextCompat.registerReceiver(this, pauseServiceReceiver, pauseFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        loadConfiguration();
        loadTodaysWastedTime();
        loadLastReminderTimestampsForAppTags();

        reminderHandler = new Handler(Looper.getMainLooper());

        preferenceChangeListener = (sharedPrefs, key) -> {
            if (key != null) {
                switch (key) {
                    case PREF_CUSTOM_BLOCKED_APPS:
                    case "keepServiceAlive":
                        updateAddonsState();
                        if ("keepServiceAlive".equals(key))
                            break; // Don't reload config for keepServiceAlive, just update addons
                    case PREF_BLOCKING_POPUP_DURATION_SEC:
                    case PREF_REMIND_DOOM_SCROLLING_ENABLED:
                    case PREF_REMIND_DOOM_SCROLLING_MINUTES:
                    case PREF_BLOCK_AFTER_WASTED_TIME_ENABLED:
                    case PREF_BLOCK_AFTER_WASTED_TIME_HOURS:
                    case PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED:
                    case PREF_BLOCK_ADULT_SITES_ENABLED:
                        loadConfiguration();
                        appViewFocusAccumulatedTimeTodayMap.clear(); // Part of old view focus
                        loadLastReminderTimestampsForAppTags();
                        break;
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        isYtHomeSwitchOn = bundle.getBoolean("home_yt_switch_on", false);
                        isInstaHomeSwitchOn = bundle.getBoolean("home_insta_switch_on", false);
                        isSnapHomeSwitchOn = bundle.getBoolean("home_snap_switch_on", false);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(IntentActions.getActionUpdatePackages(this));
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        overlayCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && ACTION_PERFORM_GLOBAL_HOME_FROM_OVERLAY.equals(intent.getAction())) {
                    performThrottledGlobalAction(GLOBAL_ACTION_HOME);
                } else if (intent != null && ACTION_PERFORM_GLOBAL_BACK_FROM_OVERLAY.equals(intent.getAction())) {
                    performThrottledGlobalAction(GLOBAL_ACTION_BACK);
                }
            }
        };
        IntentFilter overlayIntentFilter = new IntentFilter(ACTION_PERFORM_GLOBAL_HOME_FROM_OVERLAY);
        overlayIntentFilter.addAction(ACTION_PERFORM_GLOBAL_BACK_FROM_OVERLAY);
        ContextCompat.registerReceiver(this, overlayCommandReceiver, overlayIntentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        midnightStateRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && ACTION_REFRESH_DAILY_STATE_INTERNAL.equals(intent.getAction())) {
                    refreshDailyServiceState();
                }
            }
        };
        IntentFilter midnightRefreshFilter = new IntentFilter(ACTION_REFRESH_DAILY_STATE_INTERNAL);
        ContextCompat.registerReceiver(this, midnightStateRefreshReceiver, midnightRefreshFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        scheduleMidnightReset();
    }

    @Override
    protected void onServiceConnected() {
        serviceStatus = true;
        super.onServiceConnected();

        updateAddonsState();
        loadLastReminderTimestampsForAppTags();

        if (appTagToTimeLeftForNextSessionMs.isEmpty()) {
            long defaultUserReminderIntervalMs = (long) sharedPreferences.getInt(PREF_REMIND_DOOM_SCROLLING_MINUTES,
                    DEFAULT_REMIND_DOOM_SCROLLING_MINUTES) * 60 * 1000;
            for (String pkgAppTag : Utils.ALL_PACKAGES.values()) {
                if (pkgAppTag != null && !appTagToTimeLeftForNextSessionMs.containsKey(pkgAppTag)) {
                    appTagToTimeLeftForNextSessionMs.put(pkgAppTag, defaultUserReminderIntervalMs);
                }
            }
        }

        String foregroundPackageName = getForegroundPackageName();
        if (foregroundPackageName != null && Utils.ALL_PACKAGES.containsKey(foregroundPackageName)) {
            if (!foregroundPackageName.equals(this.currentPackage) || this.startTimeMillis == 0L) {
                this.currentPackage = foregroundPackageName;
                this.startTimeMillis = System.currentTimeMillis();
                if (blockAfterWastedTimeEnabled && !appTotalWastedTimeToday.containsKey(this.currentPackage)) {
                    appTotalWastedTimeToday.put(this.currentPackage,
                            (long) sharedPreferences.getInt(this.currentPackage + "_time", 0));
                }
            }
        } else {
            if (this.currentPackage != null || this.startTimeMillis != 0L) {
                this.currentPackage = null;
                this.startTimeMillis = 0L;
            }
        }
    }

    private void updateAddonsState() {
        boolean shouldKeepAlive = isKeepAlive(this);
        if (shouldKeepAlive) {
            startForegroundProtection();
            if (!isScreenReceiverRegistered) {
                registerScreenReceiver();
            }
            AlarmUtils.scheduleAlarm(this);
        } else {
            stopForeground(true);
            if (isScreenReceiverRegistered) {
                unregisterScreenReceiver();
            }
            AlarmUtils.cancelAlarm(this);
            WarningUtils.remove(this);
        }
    }

    private void unregisterScreenReceiver() {
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception e) {
                isScreenReceiverRegistered = false;
            }
        }
    }

    private void registerScreenReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        if (screenReceiver != null) {
            return;
        }
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: screenReceiver");
            }
        };
        registerReceiver(screenReceiver, filter);
        isScreenReceiverRegistered = true;
    }

    private void startForegroundProtection() {
        if (WarningUtils.isWarningVisible(this)) {
            WarningUtils.remove(this);
        }
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, FGS_CHANNEL_ID)
                .setContentTitle("MindMint Protected")
                .setContentText("Accessibility Service is Active")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setDeleteIntent(PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationDismissBroadcastReceiver.class), PendingIntent.FLAG_IMMUTABLE))
                .build();

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(FGS_NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(FGS_NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
        }
    }

    @Override
    public void onInterrupt() {
        processViewFocusEndIfActive(currentViewIdPackage); // Old view focus system
    }

    @Override
    public void onDestroy() {
        serviceStatus = false;

        if (sharedPreferences != null) {

        }
        unregisterScreenReceiver();

        if (notificationRestoreReceiver != null) {
            try {
                unregisterReceiver(notificationRestoreReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering notificationRestoreReceiver", e);
            }
        }

        super.onDestroy();
        if (packageReceiver != null) {
            unregisterReceiver(packageReceiver);
        }
        if (overlayCommandReceiver != null) {
            unregisterReceiver(overlayCommandReceiver);
        }
        if (midnightStateRefreshReceiver != null) {
            unregisterReceiver(midnightStateRefreshReceiver);
        }
        if (pauseServiceReceiver != null) {
            unregisterReceiver(pauseServiceReceiver);
        }
        if (sharedPreferences != null && preferenceChangeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
        if (currentPackage != null && Utils.ALL_PACKAGES.containsKey(currentPackage) && startTimeMillis != 0L) {
            long endTimeMillis = System.currentTimeMillis();
            int timeSpent = (int) Math.max(0L, (endTimeMillis - startTimeMillis) / 1000L);
            updateAppTimeSpent(currentPackage, timeSpent);
        }
        clearCurrentPackageTracking();
        processViewFocusEndIfActive(currentViewIdPackage); // Old view focus system

        // Clear pending tasks
        if (reminderHandler != null) {
            reminderHandler.removeCallbacksAndMessages(null);
            reminderHandler = null;
        }
        // Schedule a restart to keep the service alive
        AlarmUtils.scheduleAlarm(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = null;
        try {
            if (sharedPreferences.getBoolean("isServicePaused", false)) {
                return;
            }
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isInteractive())
                return;
            String eventPackageName = event.getPackageName() != null ? event.getPackageName().toString() : null;
            int eventType = event.getEventType();

            handleScrollCounting(event);

            // Single root node fetch for performance - reduces IPC calls
            rootNode = getRootInActiveWindow();

            if (FocusService.isPublicFocusRun && customBlockedApps != null
                    && customBlockedApps.contains(eventPackageName)) {
                Intent intent = new Intent(this, BlockingOverlayDisplayActivity.class);
                intent.putExtra(EXTRA_IS_FOCUS, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                resetUsageAndTimersForPackage(eventPackageName); // Resets general usage and specific timers for the
                                                                 // package
                return;
            }
            if (eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                if (remindDoomScrollingEnabled) {
                    String activeAppTagNow = null;
                    String activeReminderViewIdNow = null;
                    boolean isTrackedViewVisibleNow = false;

                    if (eventPackageName != null) {
                        String appTagForEvent = getAppTagFromAllPackages(eventPackageName);
                        if (appTagForEvent != null) {
                            String reminderViewId = getReminderViewIdForAppTag(appTagForEvent);
                            if (reminderViewId != null) {
                                if (isReminderViewVisible(rootNode, eventPackageName, reminderViewId)) {
                                    activeAppTagNow = appTagForEvent;
                                    activeReminderViewIdNow = reminderViewId;
                                    isTrackedViewVisibleNow = true;
                                }
                            }
                        }
                    }

                    String previouslyTrackedAppTag = currentAppTagForReminderViewTracking;

                    if (isTrackedViewVisibleNow) {
                        if (!activeAppTagNow.equals(previouslyTrackedAppTag)) {
                            if (previouslyTrackedAppTag != null) {
                                endReminderViewSession(previouslyTrackedAppTag);
                            }
                            startReminderViewSession(activeAppTagNow, activeReminderViewIdNow);
                        } else {
                            if (!activeRunnables.containsKey(activeAppTagNow)) {
                                startReminderViewSession(activeAppTagNow, activeReminderViewIdNow);
                            }
                        }
                    } else {
                        if (previouslyTrackedAppTag != null) {
                            endReminderViewSession(previouslyTrackedAppTag);
                        }
                    }
                }
                if (eventPackageName == null) {
                    if (currentPackage != null && Utils.ALL_PACKAGES.containsKey(currentPackage)
                            && startTimeMillis != 0L) {
                        long endTimeMillis = System.currentTimeMillis();
                        int timeSpent = (int) Math.max(0L, (endTimeMillis - startTimeMillis) / 1000L);
                        updateAppTimeSpent(currentPackage, timeSpent);
                    }
                    if (currentAppTagForReminderViewTracking != null) {
                        endReminderViewSession(currentAppTagForReminderViewTracking);
                    }
                    clearCurrentPackageTracking();
                    processViewFocusEndIfActive(currentViewIdPackage); // Old view focus system
                    return;
                }
                handleBlockers(eventPackageName, event);
            }

            handleBackPress(eventPackageName, rootNode, event); // Handles back press modification

            // --- Browser blocking (user list) and adult sites block ---
            if (blockBrowsersDoomEnabled || blockAdultSitesEnabled) {
                tryBlockBrowser(eventPackageName, rootNode);
            }

            // --- General App Usage Time Tracking & Window State Changes ---
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) { // NEW CONDITION (Scroll is handled above)
                if (!eventPackageName.equals(currentPackage)) {
                    if (currentPackage != null && Utils.ALL_PACKAGES.containsKey(currentPackage)
                            && startTimeMillis != 0L) {
                        long endTimeMillis = System.currentTimeMillis();
                        int timeSpent = (int) Math.max(0L, (endTimeMillis - startTimeMillis) / 1000L);
                        updateAppTimeSpent(currentPackage, timeSpent);
                    }
                    processViewFocusEndIfActive(currentPackage); // Old view focus system

                    if (Utils.ALL_PACKAGES.containsKey(eventPackageName)) {
                        currentPackage = eventPackageName;
                        startTimeMillis = System.currentTimeMillis();
                        if (blockAfterWastedTimeEnabled && !appTotalWastedTimeToday.containsKey(currentPackage)) {
                            appTotalWastedTimeToday.put(currentPackage,
                                    (long) sharedPreferences.getInt(currentPackage + "_time", 0));
                        }
                    } else {
                        clearCurrentPackageTracking();
                    }
                } else { // Same package
                    if (Utils.ALL_PACKAGES.containsKey(currentPackage)) {
                        if (startTimeMillis == 0L) { // If it was cleared, restart
                            startTimeMillis = System.currentTimeMillis();
                        }
                    } else { // Should not happen if currentPackage is set, but good for safety
                        clearCurrentPackageTracking();
                        processViewFocusEndIfActive(currentViewIdPackage); // Old view focus system
                    }
                }
            }

            // --- Global Wasted Time Blocking ---
            if (blockAfterWastedTimeEnabled) {
                long currentGlobalDailyWastedTimeMs = 0;
                for (long timeInSeconds : appTotalWastedTimeToday.values()) {
                    currentGlobalDailyWastedTimeMs += (timeInSeconds * 1000);
                }
                long globalBlockThresholdMs = (long) (blockAfterWastedTimeHours * 3600 * 1000);
                boolean globalTimeLimitExceededToday = currentGlobalDailyWastedTimeMs >= globalBlockThresholdMs;

                if (globalTimeLimitExceededToday) {
                    String appTagForGlobalBlock = getAppTagFromAllPackages(eventPackageName);
                    if (appTagForGlobalBlock != null) {
                        String viewIdToLookFor = getReminderViewIdForAppTag(appTagForGlobalBlock);

                        if (viewIdToLookFor != null && rootNode != null) {
                            List<AccessibilityNodeInfo> nodes = rootNode
                                    .findAccessibilityNodeInfosByViewId(
                                            eventPackageName + ":id/" + viewIdToLookFor);
                            boolean specificViewPresent = nodes != null && !nodes.isEmpty();
                            if (nodes != null) {
                                for (AccessibilityNodeInfo node : nodes)
                                    node.recycle();
                            }

                            if (specificViewPresent) {
                                launchOverlay(eventPackageName);
                                resetUsageAndTimersForPackage(eventPackageName);
                                return; // Important to return after blocking
                            }
                        }
                    }
                }
            }

            if (currentAppTagForReminderViewTracking != null
                    && !eventPackageName.equals(getPackageForAppTag(currentAppTagForReminderViewTracking))) {
                String appTagToEnd = currentAppTagForReminderViewTracking;
                if (reminderViewSessionStartTimeMs.containsKey(appTagToEnd)) {
                    long sessionStartTime = reminderViewSessionStartTimeMs.remove(appTagToEnd);
                    long sessionDuration = System.currentTimeMillis() - sessionStartTime;
                    if (sessionDuration > 0) {
                        long previouslyAccumulated = reminderViewAccumulatedTimeCurrentCycleMs.getOrDefault(appTagToEnd, 0L);
                        long newAccumulatedTime = previouslyAccumulated + sessionDuration;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing accessibility event", e);
        } finally {
            // Recycle the root node once at the end
            if (rootNode != null) {
                rootNode.recycle();
            }
        }
    }

    private void handleScrollCounting(AccessibilityEvent event) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : null;
        if (packageName == null) {
            return;
        }

        String appTag = getAppTagFromAllPackages(packageName);
        if (appTag == null) {
            return;
        }

        String viewId;
        int requiredEventType;
        long debounceMs;

        switch (appTag) {
            case "insta":
                viewId = Utils.instaViewId;
                requiredEventType = AccessibilityEvent.TYPE_VIEW_SCROLLED;
                debounceMs = 1000; // 1 sec
                break;
            case "yt":
                viewId = Utils.YtViewId;
                requiredEventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                debounceMs = 2500; // 2.5 sec
                break;
            case "snap":
                viewId = Utils.snapViewId;
                requiredEventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                debounceMs = 2500; // 2.5 sec
                break;
            default:
                return;
        }

        if (event.getEventType() != requiredEventType) {
            return;
        }
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (isReminderViewVisible(rootNode, packageName, viewId)) {
            rootNode.recycle();

            long currentTime = System.currentTimeMillis();
            long lastTime = lastScrollEventTimestamp.getOrDefault(appTag, 0L);
            if (currentTime - lastTime > debounceMs) {
                lastScrollEventTimestamp.put(appTag, currentTime);
                incrementScrollCount(packageName);
            }
        } else {
            if (rootNode != null)
                rootNode.recycle();
        }
    }

    private void incrementScrollCount(String packageName) {
        if (packageName == null)
            return;

        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        }

        long currentScrolls = sharedPreferences.getLong(packageName + "_scrolls", 0L);
        long newTotalScrolls = currentScrolls + 1;

        // Calculate random time spent on this scroll (reel) between 7 and 17 seconds
        int randomSeconds = (int) (Math.random() * (17 - 7 + 1) + 7);
        long currentEstimatedTime = sharedPreferences.getLong("total_estimated_wasted_time", 0);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(packageName + "_scrolls", newTotalScrolls);
        editor.putLong("total_estimated_wasted_time", currentEstimatedTime + randomSeconds);
        editor.apply();

        Intent intent = new Intent(IntentActions.getActionTimeUpdated(this));
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void handleBackPress(String currentEventPackageName, AccessibilityNodeInfo rootNode,
            AccessibilityEvent event) {
        if (rootNode == null)
            return;

        AccessibilityNodeInfo eventSource = event.getSource();
        boolean isEventSourceNull = eventSource == null;
        if (!isEventSourceNull) {
            eventSource.recycle();
        }
        if (isEventSourceNull) {
            return;
        }

        String appTag = getAppTagFromAllPackages(currentEventPackageName);
        if (appTag == null) {
            return;
        }

        boolean homeSwitchForAppIsOn;
        boolean modSwitchForAppIsOn;
        String viewIdForBackPress;

        switch (appTag) {
            case "yt":
                homeSwitchForAppIsOn = isYtHomeSwitchOn;
                modSwitchForAppIsOn = prefs.getBoolean("YtMod", false);
                viewIdForBackPress = Utils.YtViewId;
                break;
            case "insta":
                homeSwitchForAppIsOn = isInstaHomeSwitchOn;
                modSwitchForAppIsOn = prefs.getBoolean("InstaMod", false);
                viewIdForBackPress = Utils.instaViewId;
                break;
            case "snap":
                homeSwitchForAppIsOn = isSnapHomeSwitchOn;
                modSwitchForAppIsOn = prefs.getBoolean("SnapMod", false);
                viewIdForBackPress = Utils.snapViewId;
                break;
            default:
                return;
        }

        if (!homeSwitchForAppIsOn) {
            return;
        }

        // Home switch is ON, proceed with Mod switch logic
        Map<String, String> packagesToTarget = modSwitchForAppIsOn ? Utils.ALL_PACKAGES : Utils.ORIGINAL_PACKAGES;

        if (appTag.equals(packagesToTarget.get(currentEventPackageName))) {
            findAndBlockView(rootNode, viewIdForBackPress, currentEventPackageName);
        }
    }

    private class ShowReminderRunnable implements Runnable {
        private final String appTag;

        ShowReminderRunnable(String appTag) {
            this.appTag = appTag;
        }

        @Override
        public void run() {
            activeRunnables.remove(appTag);

            if (!remindDoomScrollingEnabled) {
                resetTimerStateForApp(appTag);
                return;
            }

            String currentForegroundAppTag = getAppTagFromAllPackages(getForegroundPackageName());
            if (!appTag.equals(currentForegroundAppTag)) {
                long originalDelay = appTagToOriginalDelayMs.getOrDefault(appTag, 0L);
                long postTime = appTagToLastPostTimeMs.getOrDefault(appTag, System.currentTimeMillis());
                long elapsed = System.currentTimeMillis() - postTime;
                long timeLeft = Math.max(0, originalDelay - elapsed);
                appTagToTimeLeftForNextSessionMs.put(appTag, timeLeft);
                return;
            }

            // --- PeaceCoins logic: track ignored reminders and subtract coins after 3rd
            // ---
            incrementReminderIgnoredCount(appTag);
            int ignoredCount = getReminderIgnoredCount(appTag);
            if (ignoredCount > 5) {
                try {
                    MintCrystals mintCrystals = new MintCrystals(getApplicationContext());
                    mintCrystals.subtractCoins(2);
                } catch (Exception e) {
                    Log.e(TAG, "PeaceCoins: Exception while subtracting coins for appTag " + appTag, e);
                }
            }
            // --- End PeaceCoins logic ---

            long userReminderIntervalMs = (long) remindDoomScrollingMinutes * 60 * 1000;
            long lastActualReminderShownTimeMs = lastReminderTimestampForAppTag.getOrDefault(appTag, 0L);

            if ((System.currentTimeMillis() - lastActualReminderShownTimeMs) < userReminderIntervalMs) {
                appTagToTimeLeftForNextSessionMs.put(appTag, userReminderIntervalMs);
                return;
            }

            String currentPackageName = getPackageForAppTag(appTag);

            if (currentPackageName != null) {
                Intent reminderIntent = getIntent(currentPackageName, appTag);
                try {
                    startActivity(reminderIntent);
                    saveLastReminderTimestampForAppTag(appTag, System.currentTimeMillis());
                    appTagToTimeLeftForNextSessionMs.put(appTag, userReminderIntervalMs);

                    if (appTag.equals(currentAppTagForReminderViewTracking)) {
                        currentAppTagForReminderViewTracking = null;
                    }
                    appTagToOriginalDelayMs.remove(appTag);
                    appTagToLastPostTimeMs.remove(appTag);

                } catch (Exception e) {
                    Log.e(TAG, "ShowReminderRunnable: EXCEPTION starting Reminder activity for " + appTag, e);
                }
            } else {
                appTagToTimeLeftForNextSessionMs.put(appTag, userReminderIntervalMs);
            }
        }
    }

    private void startReminderViewSession(String appTag, String reminderViewId) {
        if (appTag == null || !remindDoomScrollingEnabled) {
            return;
        }

        ShowReminderRunnable existingRunnable = activeRunnables.get(appTag);
        if (existingRunnable != null) {
            currentAppTagForReminderViewTracking = appTag;
            return;
        }

        long userReminderIntervalMs = (long) remindDoomScrollingMinutes * 60 * 1000;
        long timeToDelayMs = appTagToTimeLeftForNextSessionMs.getOrDefault(appTag, userReminderIntervalMs);

        if (timeToDelayMs <= 0) {
            timeToDelayMs = userReminderIntervalMs;
            appTagToTimeLeftForNextSessionMs.put(appTag, timeToDelayMs);
        }

        ShowReminderRunnable newRunnable = new ShowReminderRunnable(appTag);
        activeRunnables.put(appTag, newRunnable);
        reminderHandler.postDelayed(newRunnable, timeToDelayMs);
        appTagToOriginalDelayMs.put(appTag, timeToDelayMs);
        appTagToLastPostTimeMs.put(appTag, System.currentTimeMillis());
        currentAppTagForReminderViewTracking = appTag;
    }

    private void endReminderViewSession(String appTagToEnd) {
        if (appTagToEnd == null)
            return;
        ShowReminderRunnable activeRunnable = activeRunnables.remove(appTagToEnd);
        if (activeRunnable == null) {
            if (appTagToEnd.equals(currentAppTagForReminderViewTracking)) {
                currentAppTagForReminderViewTracking = null;
            }
            return;
        }

        reminderHandler.removeCallbacks(activeRunnable);

        long originalDelay = appTagToOriginalDelayMs.getOrDefault(appTagToEnd, 0L);
        long lastPostTime = appTagToLastPostTimeMs.getOrDefault(appTagToEnd, System.currentTimeMillis());
        long elapsedOnThisPost = System.currentTimeMillis() - lastPostTime;
        long timeLeftMs = Math.max(0, originalDelay - elapsedOnThisPost);

        appTagToTimeLeftForNextSessionMs.put(appTagToEnd, timeLeftMs);
        appTagToOriginalDelayMs.remove(appTagToEnd);
        appTagToLastPostTimeMs.remove(appTagToEnd);

        if (appTagToEnd.equals(currentAppTagForReminderViewTracking)) {
            currentAppTagForReminderViewTracking = null;
        }
    }

    private void resetTimerStateForApp(String appTag) {
        if (appTag == null)
            return;

        ShowReminderRunnable activeRunnable = activeRunnables.remove(appTag);
        if (activeRunnable != null) {
            reminderHandler.removeCallbacks(activeRunnable);
        }

        long userReminderIntervalMs = (long) sharedPreferences.getInt(PREF_REMIND_DOOM_SCROLLING_MINUTES,
                DEFAULT_REMIND_DOOM_SCROLLING_MINUTES) * 60 * 1000;
        appTagToTimeLeftForNextSessionMs.put(appTag, userReminderIntervalMs);
        appTagToOriginalDelayMs.remove(appTag);
        appTagToLastPostTimeMs.remove(appTag);

        if (appTag.equals(currentAppTagForReminderViewTracking)) {
            currentAppTagForReminderViewTracking = null;
        }
    }

    @NonNull
    private Intent getIntent(String currentEventPackageName, String appTag) { // Primarily used for Reminders
        Intent reminderIntent = new Intent(this, BlockingOverlayDisplayActivity.class);
        reminderIntent.putExtra(EXTRA_IS_REMINDER_ONLY, true);
        reminderIntent.putExtra(EXTRA_BLOCKED_APP_NAME, getAppNameForTag(appTag));
        reminderIntent.putExtra(EXTRA_BLOCKED_PACKAGE_NAME, currentEventPackageName);
        reminderIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return reminderIntent;
    }

    private void saveLastReminderTimestampForAppTag(String appTag, long timestamp) {
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (appTag == null || appTag.isEmpty()) {
            return;
        }
        lastReminderTimestampForAppTag.put(appTag, timestamp);
        sharedPreferences.edit().putLong(PREF_LAST_REMINDER_TIMESTAMP_APP_TAG_PREFIX + appTag, timestamp).apply();
    }

    private void loadLastReminderTimestampsForAppTags() {
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        lastReminderTimestampForAppTag.clear();
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(PREF_LAST_REMINDER_TIMESTAMP_APP_TAG_PREFIX)) {
                try {
                    String appTag = entry.getKey().substring(PREF_LAST_REMINDER_TIMESTAMP_APP_TAG_PREFIX.length());
                    long timestamp = (Long) entry.getValue();
                    if (timestamp > 0L && !appTag.isEmpty()) {
                        lastReminderTimestampForAppTag.put(appTag, timestamp);
                    }
                } catch (ClassCastException e) {
                    Log.e(TAG, "Error casting preference for " + entry.getKey(), e);
                }
            }
        }
    }

    private String getReminderViewIdForAppTag(String appTag) {
        if (appTag == null)
            return null;
        switch (appTag) {
            case "yt":
                return Utils.YtViewId;
            case "insta":
                return Utils.instaViewId;
            case "snap":
                return Utils.snapViewId;
            default:
                return null;
        }
    }

    private boolean isReminderViewVisible(AccessibilityNodeInfo rootNode, String packageName, String viewId) {
        if (rootNode == null || packageName == null || viewId == null) {
            return false;
        }
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(packageName + ":id/" + viewId);
        boolean visible = false;
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (!visible && node != null && node.isVisibleToUser()) {
                    visible = true;
                }
            }
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null)
                    node.recycle();
            }
        }
        return visible;
    }

    private void handleBlockers(String currentEventPackageName, AccessibilityEvent event) { // This handles
                                                                                            // "blockAllFeatures" view
                                                                                            // ID blocking
        boolean blockersGloballyEnabled = sharedPreferences.getBoolean("blockAllFeatures", false);
        AccessibilityNodeInfo src = event.getSource();
        boolean hasSource = src != null;
        if (src != null)
            src.recycle();
        if (!blockersGloballyEnabled || !hasSource) {
            return;
        }

        String appTag = getAppTagFromAllPackages(currentEventPackageName);
        String viewIdToBlock = appTag != null ? getReminderViewIdForAppTag(appTag) : null;

        if (viewIdToBlock != null) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                try {
                    findAndBlockView(root, viewIdToBlock, currentEventPackageName); // Uses helper
                } finally {
                    root.recycle();
                }
            }
        }
    }

    private void launchOverlay(String packageName) { // Used by Global Wasted Time Blocker
        Intent overlayIntent = new Intent();
        overlayIntent.setClassName(this, BLOCKING_OVERLAY_ACTIVITY_CLASS_NAME);
        String appName = getAppNameFromPackageManager(packageName);
        overlayIntent.putExtra(EXTRA_BLOCKED_APP_NAME, appName != null ? appName : packageName);
        overlayIntent.putExtra(EXTRA_BLOCKED_PACKAGE_NAME, packageName);
        overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            startActivity(overlayIntent);
        } catch (Exception e) {
            Log.e(TAG, "EXCEPTION starting OverlayActivity for " + packageName, e);
        }
    }

    private void loadConfiguration() {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        }
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        }
        customBlockedApps = sharedPreferences.getStringSet(PREF_CUSTOM_BLOCKED_APPS, new HashSet<>());

        boolean oldRemindDoomScrollingEnabled = remindDoomScrollingEnabled;
        int oldRemindDoomScrollingMinutes = remindDoomScrollingMinutes;

        remindDoomScrollingEnabled = sharedPreferences.getBoolean(PREF_REMIND_DOOM_SCROLLING_ENABLED, false);
        remindDoomScrollingMinutes = sharedPreferences.getInt(PREF_REMIND_DOOM_SCROLLING_MINUTES, DEFAULT_REMIND_DOOM_SCROLLING_MINUTES);
        blockAfterWastedTimeEnabled = sharedPreferences.getBoolean(PREF_BLOCK_AFTER_WASTED_TIME_ENABLED, false);
        blockAfterWastedTimeHours = sharedPreferences.getFloat(PREF_BLOCK_AFTER_WASTED_TIME_HOURS, DEFAULT_BLOCK_AFTER_WASTED_TIME_HOURS);
        blockBrowsersDoomEnabled = sharedPreferences.getBoolean(PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED, false);
        blockAdultSitesEnabled = sharedPreferences.getBoolean(PREF_BLOCK_ADULT_SITES_ENABLED, false);

        long currentUserReminderIntervalMs = (long) remindDoomScrollingMinutes * 60 * 1000;
        boolean settingsChanged = oldRemindDoomScrollingEnabled != remindDoomScrollingEnabled
                || oldRemindDoomScrollingMinutes != remindDoomScrollingMinutes;

        if (settingsChanged || appTagToTimeLeftForNextSessionMs.isEmpty()) {
            appTagToTimeLeftForNextSessionMs.clear();
            appTagToOriginalDelayMs.clear();
            appTagToLastPostTimeMs.clear();
            for (ShowReminderRunnable activeRunnable : activeRunnables.values()) {
                reminderHandler.removeCallbacks(activeRunnable);
            }
            activeRunnables.clear();

            if (remindDoomScrollingEnabled) {
                for (String appTag : Utils.ALL_PACKAGES.values()) {
                    if (appTag != null) {
                        appTagToTimeLeftForNextSessionMs.put(appTag, currentUserReminderIntervalMs);
                    }
                }
            }
        }

        if (!remindDoomScrollingEnabled) {
            if (currentAppTagForReminderViewTracking != null) {
                resetTimerStateForApp(currentAppTagForReminderViewTracking);
            }
            appTagToTimeLeftForNextSessionMs.clear();
            appTagToOriginalDelayMs.clear();
            appTagToLastPostTimeMs.clear();
            for (ShowReminderRunnable activeRunnable : activeRunnables.values()) {
                reminderHandler.removeCallbacks(activeRunnable);
            }
            activeRunnables.clear();
        }

        if (!remindDoomScrollingEnabled && !blockAfterWastedTimeEnabled) {
            currentViewFocusSessionStartTimeMap.clear();
            appViewFocusAccumulatedTimeTodayMap.clear();
            currentViewIdPackage = null;
        }
    }

    private void tryBlockBrowser(String packageName, AccessibilityNodeInfo rootNode) {
        if (packageName == null || rootNode == null)
            return;
        boolean isKnownBrowser = Utils.BROWSERS_PACKAGES.containsKey(packageName);
        if (!isKnownBrowser && !isBrowserPackage(packageName, this))
            return;

        if (isFirefox(packageName)) {
            String foundText = findUrlOrDomainFromNodeTreeLimited(rootNode);
            if (foundText == null || foundText.isEmpty()) {
                return;
            }
            String lowerText = foundText.toLowerCase();
            String host = AdultDomainListManager.extractHostFromUrlText(lowerText);
            if (host == null || !host.contains(".")) {
                return;
            }

            Set<String> candidateTexts = new HashSet<>();
            candidateTexts.add(lowerText);
            Set<String> candidateHosts = new HashSet<>();
            candidateHosts.add(host);

            if (blockBrowsersDoomEnabled) {
                if (isBlockedByUserLists(candidateTexts, candidateHosts)) {
                    performThrottledGlobalAction(GLOBAL_ACTION_BACK);
                    launchOverlay(packageName);
                    resetUsageAndTimersForPackage(packageName);
                    return;
                }
            }
            if (blockAdultSitesEnabled) {
                boolean isBlocked = AdultDomainListManager.isAdultHost(this, host);
                if (isBlocked) {
                    performThrottledGlobalAction(GLOBAL_ACTION_BACK);
                    launchOverlay(packageName);
                    resetUsageAndTimersForPackage(packageName);
                    return;
                }
            }
            // If not blocked, do nothing further for Firefox
            return;
        }

        Pair<String, Boolean> result = tryGetUrlBarTextWithRetry(packageName, rootNode);
        String urlText = result.first;

        if (urlText == null || urlText.isEmpty())
            return;

        String lowerText = urlText.toLowerCase();
        String host = AdultDomainListManager.extractHostFromUrlText(lowerText);
        Set<String> candidateTexts = new HashSet<>();
        candidateTexts.add(lowerText);
        Set<String> candidateHosts = new HashSet<>();
        if (host != null && host.contains("."))
            candidateHosts.add(host);

        // 1) User-defined blocking (no doom fallback)
        if (blockBrowsersDoomEnabled) {
            if (isEdgeOrSamsung(packageName)) {
                if (isBlockedByUserListsEdgeSamsung(candidateTexts, candidateHosts)) {
                    performThrottledGlobalAction(GLOBAL_ACTION_BACK);
                    launchOverlay(packageName);
                    resetUsageAndTimersForPackage(packageName);
                    return;
                }
            } else {
                if (isBlockedByUserLists(candidateTexts, candidateHosts)) {
                    performThrottledGlobalAction(GLOBAL_ACTION_BACK);
                    launchOverlay(packageName);
                    resetUsageAndTimersForPackage(packageName);
                    return;
                }
            }
        }

        // 2) Adult sites blocking using URL text only
        if (blockAdultSitesEnabled && !candidateHosts.isEmpty()) {
            for (String h : candidateHosts) {
                boolean shouldCheck = (lastAdultCheckedDomain == null) || !lastAdultCheckedDomain.equals(h);
                boolean isBlocked = lastAdultCheckedBlocked;
                if (shouldCheck) {
                    isBlocked = AdultDomainListManager.isAdultHost(this, h);
                    lastAdultCheckedDomain = h;
                    lastAdultCheckedBlocked = isBlocked;
                }
                if (isBlocked) {
                    performThrottledGlobalAction(GLOBAL_ACTION_BACK);
                    launchOverlay(packageName);
                    resetUsageAndTimersForPackage(packageName);
                    return;
                }
            }
        }
    }

    private boolean isFirefox(String packageName) {
        if (packageName == null)
            return false;
        if (packageName.startsWith("org.mozilla.firefox"))
            return true;
        if ("org.mozilla.firefox_beta".equals(packageName))
            return true;
        return "org.mozilla.fenix".equals(packageName); // Fenix-based Firefox
    }

    @Nullable
    private String findUrlOrDomainFromNodeTreeLimited(AccessibilityNodeInfo root) {
        if (root == null)
            return null;
        ArrayDeque<AccessibilityNodeInfo> queue = new java.util.ArrayDeque<>();
        queue.add(root);
        int visited = 0;

        while (!queue.isEmpty() && visited < 50) {
            AccessibilityNodeInfo node = queue.pollFirst();
            if (node == null)
                continue;
            visited++;

            // Examine text-like fields
            CharSequence t = node.getText();
            if (t != null) {
                String s = t.toString();
                String host = AdultDomainListManager.extractHostFromUrlText(s.toLowerCase());
                if (host != null && host.contains(".")) {
                    node.recycle();
                    return s;
                }
            }
            CharSequence cd = node.getContentDescription();
            if (cd != null) {
                String s = cd.toString();
                String host = AdultDomainListManager.extractHostFromUrlText(s.toLowerCase());
                if (host != null && host.contains(".")) {
                    node.recycle();
                    return s;
                }
            }

            // Traverse children
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    queue.add(child);
                }
            }
            node.recycle();
        }

        return null;
    }

    private boolean isBrowserPackage(String packageName, Context context) {
        Boolean cached = browserCheckCache.get(packageName);
        if (cached != null)
            return cached;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        boolean isBrowser = false;
        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo.packageName.equals(packageName)) {
                isBrowser = true;
                break;
            }
        }
        browserCheckCache.put(packageName, isBrowser);
        return isBrowser;
    }

    private Pair<String, Boolean> tryGetUrlBarText(String packageName, AccessibilityNodeInfo rootNode) {
        boolean nodeFound = false;
        String id = Utils.BROWSERS_PACKAGES.get(packageName);
        if (id != null) {
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(packageName + ":id/" + id);
            if (nodes != null && !nodes.isEmpty()) {
                nodeFound = true;
                String foundText = null;
                for (AccessibilityNodeInfo n : nodes) {
                    if (n == null)
                        continue;
                    CharSequence t = n.getText();
                    if (t != null && t.length() > 0) {
                        foundText = t.toString();
                        break;
                    }
                }
                for (AccessibilityNodeInfo n : nodes) {
                    if (n != null)
                        n.recycle();
                }
                if (foundText != null)
                    return new Pair<>(foundText, true);
            }
        }
        for (String candidateId : Utils.browserIds) {
            List<AccessibilityNodeInfo> nodes = rootNode
                    .findAccessibilityNodeInfosByViewId(packageName + ":id/" + candidateId);
            if (nodes == null || nodes.isEmpty())
                continue;
            nodeFound = true;
            String foundText = null;
            for (AccessibilityNodeInfo n : nodes) {
                if (n == null)
                    continue;
                CharSequence t = n.getText();
                if (t != null && t.length() > 0) {
                    foundText = t.toString();
                    break;
                }
            }
            for (AccessibilityNodeInfo n : nodes) {
                if (n != null)
                    n.recycle();
            }
            if (foundText != null)
                return new Pair<>(foundText, true);
        }
        if (nodeFound)
            return new Pair<>("", true);
        return new Pair<>(null, false);
    }

    private Pair<String, Boolean> tryGetUrlBarTextWithRetry(String packageName, AccessibilityNodeInfo rootNode) {
        final int maxRetries = 3;
        final int delayMs = 300;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return tryGetUrlBarText(packageName, rootNode);
        }

        // Only retry if we're on a background thread (shouldn't happen in normal flow)
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Pair<String, Boolean> result = tryGetUrlBarText(packageName, rootNode);
            if (result.second) {
                return result;
            }
            // Double-check we're not on main thread before sleeping
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return result; // Exit immediately if somehow on main thread
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                Log.d("Sleep", "Interrupted during retry", e);
                return result; // Return current result on interrupt
            }
        }

        // No node found after all retries
        return new Pair<>(null, false);
    }

    private boolean isEdgeOrSamsung(String packageName) {
        if (packageName == null)
            return false;
        if (packageName.startsWith("com.microsoft.emmx"))
            return true;
        return "com.sec.android.app.sbrowser".equals(packageName);
    }

    private boolean isBlockedByUserListsEdgeSamsung(Set<String> candidateTextsLower, Set<String> candidateHosts) {
        Set<String> domains = BlockedSitesManager.getBlockedDomains(this);
        Set<String> exacts = BlockedSitesManager.getBlockedExactUrls(this);

        for (String d : domains) {
            if (d == null)
                continue;
            String dl = d.toLowerCase();
            boolean domainLike = dl.contains(".");
            if (domainLike) {
                if (candidateHosts != null) {
                    for (String host : candidateHosts) {
                        if (host.equals(dl) || host.endsWith("." + dl)) {
                            return true;
                        }
                    }
                }
            } else {
                if (candidateTextsLower != null) {
                    for (String ct : candidateTextsLower) {
                        if (ct.contains(dl)) {
                            return true;
                        }
                    }
                }
            }
        }

        for (String e : exacts) {
            if (e == null)
                continue;
            HostPath exactHp = parseHostPath(e);
            if (exactHp == null || exactHp.host == null)
                continue;
            if (candidateHosts != null) {
                for (String host : candidateHosts) {
                    if (host.equals(exactHp.host) || host.endsWith("." + exactHp.host)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isBlockedByUserLists(Set<String> candidateTextsLower, Set<String> candidateHosts) {
        Set<String> domains = BlockedSitesManager.getBlockedDomains(this);
        Set<String> exacts = BlockedSitesManager.getBlockedExactUrls(this);

        // Domain entries
        for (String d : domains) {
            if (d == null)
                continue;
            String dl = d.toLowerCase();
            boolean domainLike = dl.contains(".");
            if (domainLike) {
                // Require host and check suffix match
                if (candidateHosts != null) {
                    for (String host : candidateHosts) {
                        if (host.equals(dl) || host.endsWith("." + dl)) {
                            return true;
                        }
                    }
                }
            } else {
                // Brand keyword match limited to URL bar/title strings only
                if (candidateTextsLower != null) {
                    for (String ct : candidateTextsLower) {
                        if (ct.contains(dl)) {
                            return true;
                        }
                    }
                }
            }
        }

        // Exact URL entries (host + path prefix match)
        for (String e : exacts) {
            if (e == null)
                continue;
            HostPath exactHp = parseHostPath(e);
            if (exactHp == null || exactHp.host == null)
                continue;
            if (candidateTextsLower != null) {
                for (String ct : candidateTextsLower) {
                    HostPath candHp = parseHostPath(ct);
                    if (candHp == null || candHp.host == null)
                        continue;
                    if (candHp.host.equals(exactHp.host) || candHp.host.endsWith("." + exactHp.host)) {
                        if (candHp.path.startsWith(exactHp.path)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static class HostPath {
        String host;
        String path;
    }

    @Nullable
    private HostPath parseHostPath(String text) {
        if (text == null || text.isEmpty())
            return null;
        String host = AdultDomainListManager.extractHostFromUrlText(text);
        if (host == null || !host.contains("."))
            return null;
        String s = text.trim();
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s;
        }
        try {
            java.net.URL u = new java.net.URL(s);
            String h = u.getHost().toLowerCase();
            if (h.startsWith("www."))
                h = h.substring(4);
            String p = u.getPath();
            if (p == null || p.isEmpty())
                p = "/";
            if (p.length() > 1 && p.endsWith("/"))
                p = p.substring(0, p.length() - 1);
            HostPath hp = new HostPath();
            hp.host = h;
            hp.path = p;
            return hp;
        } catch (Exception ignore) {
            String p = "/";
            int slash = s.indexOf('/');
            if (slash >= 0)
                p = s.substring(slash);
            if (p.length() > 1 && p.endsWith("/"))
                p = p.substring(0, p.length() - 1);
            HostPath hp = new HostPath();
            hp.host = host;
            hp.path = p;
            return hp;
        }
    }

    private void logNodeTree(AccessibilityNodeInfo node, int depth) {
        if (node == null)
            return;
        String indent = new String(new char[depth]).replace("\0", "-");
        Log.d("NodeDump", indent + node.getViewIdResourceName() + " | " + node.getText());
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            logNodeTree(child, depth + 1);
            if (child != null)
                child.recycle();
        }
    }

    @Nullable
    private String getForegroundPackageName() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        String foregroundPackageName = null;
        if (rootNode != null) {
            CharSequence packageNameChars = rootNode.getPackageName();
            if (packageNameChars != null) {
                foregroundPackageName = packageNameChars.toString();
            }
            rootNode.recycle();
        }
        return foregroundPackageName;
    }

    private void scheduleMidnightReset() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MidnightResetReceiver.class);
        intent.setAction(IntentActions.getActionResetAppTimes(this));
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY,
                pendingIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    private void clearCurrentPackageTracking() {
        if (currentPackage != null) {
            Log.d(TAG, "Clearing general app usage tracking for '" + currentPackage + "'.");
        }
        currentPackage = null;
        startTimeMillis = 0L;
    }

    private void resetUsageAndTimersForPackage(String packageName) {
        if (packageName == null)
            return;

        if (packageName.equals(this.currentPackage)) {
            clearCurrentPackageTracking();
        }
        String appTag = getAppTagFromAllPackages(packageName);
        if (appTag != null) {
            resetTimerStateForApp(appTag);
        }

        processViewFocusEndIfActive(packageName); // Old view focus system
    }

    private void updateAppTimeSpent(String packageName, int timeSpentSeconds) {
        if (timeSpentSeconds <= 0)
            return;
        int currentTime = sharedPreferences.getInt(packageName + "_time", 0);
        int newTotalTime = currentTime + timeSpentSeconds;
        sharedPreferences.edit().putInt(packageName + "_time", newTotalTime).apply();
        appTotalWastedTimeToday.put(packageName, (long) newTotalTime);

        Intent intent = new Intent(IntentActions.getActionTimeUpdated(this));
        intent.putExtra("packageName", packageName);
        intent.putExtra("timeSpent", timeSpentSeconds);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void processViewFocusEndIfActive(String packageName) { // Part of the old view focus system
        if (packageName != null && packageName.equals(currentViewIdPackage)) {
            currentViewIdPackage = null;
        }
    }

    public static void resetDailyViewTracking(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> keysToRemove = new HashSet<>();
        SharedPreferences focusPref = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor1 = focusPref.edit();
        editor1.remove("TotalFocusedTime");
        editor1.apply();

        for (String pkgName : Utils.ALL_PACKAGES.keySet()) {
            String timeKeyToClear = pkgName + "_time";
            if (prefs.contains(timeKeyToClear)) {
                keysToRemove.add(timeKeyToClear);
            }
            String scrollsKeyToClear = pkgName + "_scrolls";
            if (prefs.contains(scrollsKeyToClear)) {
                keysToRemove.add(scrollsKeyToClear);
            }
            // Also clear reminder ignored count for each appTag
            String appTag = Utils.ALL_PACKAGES.get(pkgName);
            if (appTag != null) {
                String ignoreKey = PREF_REMINDER_IGNORED_COUNT_PREFIX + appTag;
                if (prefs.contains(ignoreKey)) {
                    keysToRemove.add(ignoreKey);
                }
            }
        }

        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(PREF_APP_VIEW_ACCUMULATED_TIME_MS_PREFIX) || // Old view focus
                    key.startsWith(PREF_LAST_VIEW_REMINDER_TIMESTAMP_PREFIX) || // Old view focus
                    key.startsWith(PREF_LAST_REMINDER_TIMESTAMP_APP_TAG_PREFIX) || // New reminder cooldowns
                    key.startsWith(PREF_REMINDER_VIEW_ACCUMULATED_TIME_CYCLE_MS_APP_TAG_PREFIX)) { // Old reminder
                                                                                                   // system
                keysToRemove.add(key);
            }
        }

        if (!keysToRemove.isEmpty()) {
            for (String key : keysToRemove) {
                editor.remove(key);
            }
            editor.apply();
        }

        Intent intent = new Intent(ACTION_REFRESH_DAILY_STATE_INTERNAL);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    private void loadTodaysWastedTime() {
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        appTotalWastedTimeToday.clear();
        for (String pkgName : Utils.ALL_PACKAGES.keySet()) {
            int time = sharedPreferences.getInt(pkgName + "_time", 0);
            if (time > 0) {
                appTotalWastedTimeToday.put(pkgName, (long) time);
            }
        }
    }

    private void refreshDailyServiceState() {
        loadTodaysWastedTime();
        loadLastReminderTimestampsForAppTags();
        resetAllReminderIgnoredCounts();

        long currentUserReminderIntervalMs = (long) remindDoomScrollingMinutes * 60 * 1000;
        appTagToTimeLeftForNextSessionMs.clear();
        appTagToOriginalDelayMs.clear();
        appTagToLastPostTimeMs.clear();
        for (ShowReminderRunnable activeRunnable : activeRunnables.values()) {
            reminderHandler.removeCallbacks(activeRunnable);
        }
        activeRunnables.clear();

        if (remindDoomScrollingEnabled) {
            for (String appTag : Utils.ALL_PACKAGES.values()) {
                if (appTag != null) {
                    appTagToTimeLeftForNextSessionMs.put(appTag, currentUserReminderIntervalMs);
                }
            }
        }
    }

    private String getAppNameFromPackageManager(String packageName) {
        PackageManager pm = getApplicationContext().getPackageManager();
        String appName;
        try {
            appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA))
                    .toString();
        } catch (PackageManager.NameNotFoundException e) {
            appName = packageName;
        }
        return appName;
    }

    private String getAppTagFromAllPackages(String packageName) {
        if (Utils.ALL_PACKAGES.containsKey(packageName)) {
            return Utils.ALL_PACKAGES.get(packageName);
        }
        return null;
    }

    private String getPackageForAppTag(String appTag) { // Helper for reminders
        if (appTag == null)
            return null;
        for (Map.Entry<String, String> entry : Utils.ALL_PACKAGES.entrySet()) {
            if (appTag.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String getAppNameForTag(String tag) { // Helper for reminder/overlay UI
        switch (tag) {
            case "yt":
                return "YouTube";
            case "insta":
                return "Instagram";
            case "snap":
                return "Snapchat";
            default:
                return "the app";
        }
    }

    private void findAndBlockView(AccessibilityNodeInfo rootNode, String targetViewIdName, String packageName) {
        if (rootNode == null || targetViewIdName == null || packageName == null) {
            if (rootNode != null)
                rootNode.recycle();
            return;
        }
        List<AccessibilityNodeInfo> nodes = rootNode
                .findAccessibilityNodeInfosByViewId(packageName + ":id/" + targetViewIdName);
        if (nodes != null && !nodes.isEmpty()) {
            boolean actionPerformed = false;
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null && node.isVisibleToUser() && !actionPerformed) {
                    performThrottledGlobalAction(GLOBAL_ACTION_BACK);
                    actionPerformed = true;
                }
                if (node != null)
                    node.recycle();
            }
        }
    }

    private void performThrottledGlobalAction(int action) {
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 800)
            return;
        lastActionTime = now;
        performGlobalAction(action);
    }

    private int getReminderIgnoredCount(String appTag) {
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getInt(PREF_REMINDER_IGNORED_COUNT_PREFIX + appTag, 0);
    }

    private void incrementReminderIgnoredCount(String appTag) {
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int count = getReminderIgnoredCount(appTag) + 1;
        sharedPreferences.edit().putInt(PREF_REMINDER_IGNORED_COUNT_PREFIX + appTag, count).apply();
    }

    private void resetAllReminderIgnoredCounts() {
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String appTag : Utils.ALL_PACKAGES.values()) {
            editor.putInt(PREF_REMINDER_IGNORED_COUNT_PREFIX + appTag, 0);
        }
        editor.apply();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        serviceStatus = false;
        return super.onUnbind(intent);
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel serviceChannel = new NotificationChannel(
                FGS_CHANNEL_ID,
                "Mind Mint Service",
                NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(serviceChannel);
    }
}
