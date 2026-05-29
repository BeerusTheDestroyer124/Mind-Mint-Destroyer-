package com.gxdevs.mindmint.Services;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NotificationOptimizer {
    
    private static final String TAG = "NotificationOptimizer";
    private final Context context;
    private final NotificationManager notificationManager;
    private List<Integer> quietHours;
    private boolean isDNDEnabled;
    
    public NotificationOptimizer(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.quietHours = new ArrayList<>();
        setDefaultQuietHours();
    }
    
    private void setDefaultQuietHours() {
        for (int i = 22; i < 24; i++) quietHours.add(i);
        for (int i = 0; i < 8; i++) quietHours.add(i);
    }
    
    public void setQuietHours(int startHour, int endHour) {
        quietHours.clear();
        if (startHour <= endHour) {
            for (int i = startHour; i <= endHour; i++) quietHours.add(i);
        } else {
            for (int i = startHour; i < 24; i++) quietHours.add(i);
            for (int i = 0; i <= endHour; i++) quietHours.add(i);
        }
        Log.d(TAG, "Quiet hours set: " + startHour + " - " + endHour);
    }
    
    public boolean isInQuietHours() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        return quietHours.contains(currentHour);
    }
    
    public boolean shouldShowNotification(int priority) {
        if (isDNDEnabled && isInQuietHours()) {
            return priority >= NotificationCompat.PRIORITY_HIGH;
        }
        return true;
    }
    
    public void enableDND(boolean enable) {
        isDNDEnabled = enable;
        Log.d(TAG, "DND: " + (enable ? "enabled" : "disabled"));
    }
    
    public long getNextOptimalNotificationTime() {
        Calendar calendar = Calendar.getInstance();
        if (isInQuietHours()) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            return calendar.getTimeInMillis();
        }
        return System.currentTimeMillis();
    }
    
    public String getOptimizationStatus() {
        return "Quiet Hours Enabled | DND: " + (isDNDEnabled ? "ON" : "OFF") + 
               " | Current Time In Quiet Hours: " + isInQuietHours();
    }
}
