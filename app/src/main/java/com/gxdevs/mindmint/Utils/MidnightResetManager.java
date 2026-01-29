package com.gxdevs.mindmint.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.gxdevs.mindmint.Services.AppUsageAccessibilityService;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.DailyStatsDao;
import com.gxdevs.mindmint.db.entities.DailyStatsEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MidnightResetManager {
    private static final String TAG = "MidnightResetManager";
    private static final String KEY_LAST_RESET_DATE = "last_midnight_reset_date";

    public static void checkAndPerformReset(Context context) {
        if (shouldReset(context)) {
            Log.d(TAG, "New day detected. Performing midnight reset.");
            performReset(context);
        } else {
            Log.d(TAG, "Already reset for today.");
        }
    }

    private static boolean shouldReset(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "");
        String todayDate = DateUtils.getTodayDateString();
        return !todayDate.equals(lastResetDate);
    }

    public static void performReset(Context context) {
        Log.d(TAG, "Starting performReset...");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(context);

        long totalScreenTimeSec = 0;
        long totalScrolls = 0;

        // Iterate all prefs to find usage data
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.endsWith("_time") && value instanceof Integer) {
                totalScreenTimeSec += (Integer) value;
            } else if (key.endsWith("_scrolls") && value instanceof Long) {
                totalScrolls += (Long) value;
            }
        }

        // Save to DailyStats for Yesterday
        String yesterday = DateUtils.getYesterdayDateString();
        try {
            DailyStatsDao dailyStatsDao = db.dailyStatsDao();
            DailyStatsEntity stats = dailyStatsDao.getByDate(yesterday);

            if (stats == null) {
                stats = new DailyStatsEntity();
                stats.date = yesterday;
            }

            stats.total_screen_time_seconds = totalScreenTimeSec;
            stats.total_scrolls = totalScrolls;

            long yesterdayStart = DateUtils.getStartOfDayMs(yesterday);
            long yesterdayEnd = DateUtils.getEndOfDayMs(yesterday);

            int tasksCompleted = db.taskDao().getCompletedCountForDate(yesterdayStart, yesterdayEnd);
            int habitsCompleted = db.habitCompletionDao().getUniqueHabitsCompletedForDate(yesterdayStart, yesterdayEnd);

            stats.tasks_completed = tasksCompleted;
            stats.habits_completed = habitsCompleted;

            dailyStatsDao.insertOrReplace(stats);
            Log.d(TAG, "Saved Yesterday's stats: ScreenTime=" + totalScreenTimeSec + "s, Scrolls=" + totalScrolls);

        } catch (Exception e) {
            Log.e(TAG, "Error saving daily stats", e);
        }

        Set<String> keysToRemove = new HashSet<>();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Re-scan for keys to remove (time, scrolls, internal flags)
        for (String key : allEntries.keySet()) {
            if (key.endsWith("_time") || key.endsWith("_scrolls") ||
                    key.startsWith("app_view_accumulated_time_ms_") ||
                    key.startsWith("last_view_reminder_timestamp_") ||
                    key.startsWith("last_reminder_timestamp_app_tag_") ||
                    key.startsWith("reminder_view_accumulated_time_cycle_ms_app_tag_") ||
                    key.startsWith("reminder_ignored_count_") ||
                    key.equals("TotalFocusedTime")) {

                keysToRemove.add(key);
            }
        }

        if (!keysToRemove.isEmpty()) {
            for (String key : keysToRemove) {
                editor.remove(key);
            }
            editor.apply();
            Log.d(TAG, "Cleared " + keysToRemove.size() + " preference keys.");
        }

        try {
            long todayStart = DateUtils.getStartOfTodayMs();
            db.habitDao().resetDailyHabitProgress(todayStart);
            Log.d(TAG, "Reset daily habit progress.");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting habit progress", e);
        }

        try {
            StatsManager statsManager = new StatsManager(context);
            statsManager.checkAndUpdateStreaksAtMidnight();
            statsManager.updateTodayStats(); // This initializes Today's empty row
            Log.d(TAG, "Updated streaks and daily stats.");
        } catch (Exception e) {
            Log.e(TAG, "Error updating streaks/stats", e);
        }

        // If service is running, it needs to reload from the (now empty) prefs
        Intent internalRefreshIntent = new Intent(AppUsageAccessibilityService.ACTION_REFRESH_DAILY_STATE_INTERNAL);
        internalRefreshIntent.setPackage(context.getPackageName());
        context.sendBroadcast(internalRefreshIntent);

        sharedPreferences.edit().putString(KEY_LAST_RESET_DATE, DateUtils.getTodayDateString()).apply();

        Log.d(TAG, "Midnight reset completed successfully.");
    }
}
