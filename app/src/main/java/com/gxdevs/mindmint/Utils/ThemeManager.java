package com.gxdevs.mindmint.Utils;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * ThemeManager - Handles dark mode and theme switching
 */
public class ThemeManager {
    
    private static final String THEME_PREF_KEY = "app_theme_mode";
    private static final String SYSTEM_THEME = "system";
    private static final String DARK_THEME = "dark";
    private static final String LIGHT_THEME = "light";
    
    private static SharedPreferences sharedPreferences;
    
    public ThemeManager(Application application) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }
    
    /**
     * Set theme mode
     * @param mode "system", "dark", or "light"
     */
    public static void setTheme(String mode) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(THEME_PREF_KEY, mode).apply();
        }
        
        switch (mode) {
            case DARK_THEME:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case LIGHT_THEME:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SYSTEM_THEME:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
        }
    }
    
    /**
     * Get current theme mode
     */
    public static String getCurrentTheme() {
        if (sharedPreferences != null) {
            return sharedPreferences.getString(THEME_PREF_KEY, SYSTEM_THEME);
        }
        return SYSTEM_THEME;
    }
    
    /**
     * Check if dark mode is active
     */
    public static boolean isDarkModeEnabled(Application application) {
        int nightMode = application.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Get color based on theme
     */
    public static int getThemedColor(Application application, int lightColor, int darkColor) {
        if (isDarkModeEnabled(application)) {
            return darkColor;
        } else {
            return lightColor;
        }
    }
}
