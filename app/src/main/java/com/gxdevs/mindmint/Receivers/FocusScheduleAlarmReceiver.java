package com.gxdevs.mindmint.Receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.gxdevs.mindmint.Activities.FocusMode;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Services.FocusService;
import com.gxdevs.mindmint.Utils.FocusScheduleManager;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.entities.FocusScheduleEntity;

/**
 * Handles the alarm trigger.
 * Shows a heads-up notification with a Cancel option.
 * If not cancelled within 3 seconds, automatically starts Focus Mode.
 */
public class FocusScheduleAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "FocusScheduleAlarm";
    public static final String EXTRA_SCHEDULE_ID = "scheduleId";
    public static final String ACTION_CANCEL = "com.gxdevs.mindmint.action.CANCEL_SCHEDULE_FIRE";

    private static final int NOTIF_ID = 2001;
    private static final String CHANNEL_ID = "ScheduledFocusChannel";

    // Keep track of active tasks so we can cancel them
    // volatile: written from background thread (onReceive), read from main thread (cancel path)
    private static volatile Runnable autoStartTask;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static volatile int activeScheduleId = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_CANCEL.equals(intent.getAction())) {
            Log.d(TAG, "User cancelled scheduled focus session.");
            if (autoStartTask != null) {
                handler.removeCallbacks(autoStartTask);
                autoStartTask = null;
            }
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIF_ID);

            // BUG-7 FIX: reschedule does a Room DB lookup — must run off the main thread
            if (activeScheduleId != -1) {
                final int idToReschedule = activeScheduleId;
                activeScheduleId = -1;
                new Thread(() -> reschedule(context, idToReschedule)).start();
            }
            return;
        }

        final int scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, -1);
        if (scheduleId == -1) return;

        // Run fetch in background
        final PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                FocusScheduleEntity schedule = MindMintRoomDatabase.getInstance(context)
                        .focusScheduleDao().getById(scheduleId);

                if (schedule == null || schedule.isEnabled == 0) {
                    return; // Deleted or disabled
                }

                // Show Heads-Up Notification
                showPreNotification(context, schedule);

                // Schedule auto-start after 10 seconds
                activeScheduleId = scheduleId;
                autoStartTask = () -> {
                    Log.d(TAG, "10 seconds passed, auto-starting focus session");
                    startFocusSession(context, schedule);
                    
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) nm.cancel(NOTIF_ID);
                    
                    reschedule(context, scheduleId);
                    activeScheduleId = -1;
                    autoStartTask = null;
                };

                handler.postDelayed(autoStartTask, 10_000L);

            } finally {
                pendingResult.finish();
            }
        }).start();
    }

    private void showPreNotification(Context context, FocusScheduleEntity schedule) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Scheduled Focus",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications before auto-starting scheduled focus sessions");
        nm.createNotificationChannel(channel);

        Intent cancelIntent = new Intent(context, FocusScheduleAlarmReceiver.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent cancelPi = PendingIntent.getBroadcast(
                context, schedule.id, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long durationMs = schedule.durationMinutes * 60000L;
        String label = schedule.label != null && !schedule.label.isEmpty() ? schedule.label : "Scheduled Session";
        String title = "🧠 Focus Time: " + label;
        String text = "Starting in 10 seconds — tap Cancel to skip this session.";

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.focus_yoga)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(R.drawable.x, "Cancel Session", cancelPi)
                .build();

        nm.notify(NOTIF_ID, notification);
    }

    private void startFocusSession(Context context, FocusScheduleEntity schedule) {
        // Start FocusService
        long durationMs = schedule.durationMinutes * 60000L;
        Intent serviceIntent = new Intent(context, FocusService.class);
        serviceIntent.setAction(FocusService.ACTION_START_FOREGROUND_SERVICE);
        serviceIntent.putExtra("durationInMillis", durationMs);
        serviceIntent.putExtra(FocusService.EXTRA_IS_LOCKED_IN, schedule.isLockedIn == 1);
        serviceIntent.putExtra("topicName", schedule.label != null && !schedule.label.isEmpty() ? schedule.label : "Scheduled Focus");

        // Pass global Pomodoro configurations
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        boolean isPomodoro = prefs.getBoolean("pref_pomodoro_enabled", false);
        long focusInterval = prefs.getInt("pref_focus_duration", 25) * 60 * 1000L;
        long breakInterval = prefs.getInt("pref_break_duration", 5) * 60 * 1000L;
        
        serviceIntent.putExtra("isPomodoroEnabled", isPomodoro);
        serviceIntent.putExtra("pomodoroFocusInterval", focusInterval);
        serviceIntent.putExtra("pomodoroBreakInterval", breakInterval);

        context.startForegroundService(serviceIntent);

        // Open FocusMode activity
        Intent focusIntent = new Intent(context, FocusMode.class);
        focusIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(focusIntent);
    }

    private void reschedule(Context context, int scheduleId) {
        FocusScheduleManager manager = new FocusScheduleManager(context);
        FocusScheduleEntity schedule = MindMintRoomDatabase.getInstance(context)
                .focusScheduleDao().getById(scheduleId);
        if (schedule != null) {
            manager.setSchedule(schedule);
        }
    }
}
