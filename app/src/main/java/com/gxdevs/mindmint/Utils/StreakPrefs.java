package com.gxdevs.mindmint.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class StreakPrefs {
    private static final String PREF_NAME = "streak_prefs";
    private static final String KEY_STREAK_COUNT = "streak_count";
    private final SharedPreferences prefs;

    public StreakPrefs(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getStreak() {
        return prefs.getInt(KEY_STREAK_COUNT, 0);
    }

    public void setStreak(int streak) {
        prefs.edit().putInt(KEY_STREAK_COUNT, streak).apply();
    }

}
