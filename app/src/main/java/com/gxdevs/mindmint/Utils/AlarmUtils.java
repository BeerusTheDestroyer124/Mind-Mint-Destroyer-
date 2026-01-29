package com.gxdevs.mindmint.Utils;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.gxdevs.mindmint.Utils.Utils.isKeepAlive;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.gxdevs.mindmint.Receivers.AccessibilityCheckReceiver;

public class AlarmUtils {

    public static final int REQUEST_CODE = 9201;

    public static void scheduleAlarm(Context context) {
        if (!isKeepAlive(context)) {
            cancelAlarm(context);
            return;
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AccessibilityCheckReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT);

        long interval = 20 * 60 * 1000; // 20 minutes
        long trigger = System.currentTimeMillis() + interval;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else {
                // Fallback to inexact alarm when exact alarm permission not granted
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
        }

    }

    public static void cancelAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AccessibilityCheckReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}
