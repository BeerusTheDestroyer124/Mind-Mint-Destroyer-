package com.gxdevs.mindmint.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gxdevs.mindmint.Receivers.FocusScheduleAlarmReceiver;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.entities.FocusScheduleEntity;

import java.util.Calendar;
import java.util.List;

/**
 * Manages AlarmManager alarms for recurring focus schedules.
 * Uses existing exact alarm permissions already requested in the app.
 */
public class FocusScheduleManager {

    private static final String TAG = "FocusScheduleManager";

    private final Context context;
    private final AlarmManager alarmManager;

    public FocusScheduleManager(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /** Schedule (or re-schedule) an alarm for the given schedule entity. */
    public void setSchedule(FocusScheduleEntity schedule) {
        if (schedule.isEnabled != 1) return;
        long triggerAtMs = getNextTriggerTimeMs(schedule);
        if (triggerAtMs <= 0) {
            Log.w(TAG, "No upcoming trigger day for schedule " + schedule.id);
            return;
        }

        PendingIntent pi = buildPendingIntent(schedule.id);
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            Log.d(TAG, "Scheduled focus alarm for schedule " + schedule.id + " at " + triggerAtMs);
        } catch (SecurityException e) {
            Log.e(TAG, "Cannot schedule exact alarm", e);
        }
    }

    /** Cancel the alarm for a given schedule id. */
    public void cancelSchedule(int scheduleId) {
        PendingIntent pi = buildPendingIntent(scheduleId);
        alarmManager.cancel(pi);
        pi.cancel();
        Log.d(TAG, "Cancelled focus alarm for schedule " + scheduleId);
    }

    /** Re-register all enabled schedules (e.g. after device reboot). */
    public void rescheduleAll() {
        List<FocusScheduleEntity> enabled = MindMintRoomDatabase.getInstance(context)
                .focusScheduleDao().getEnabled();
        for (FocusScheduleEntity s : enabled) {
            setSchedule(s);
        }
    }

    /**
     * Calculate next trigger time (alarm fires 3 seconds BEFORE the desired start).
     * Returns -1 if no matching day found within 7 days.
     */
    public long getNextTriggerTimeMs(FocusScheduleEntity schedule) {
        if (schedule.daysOfWeek == null || schedule.daysOfWeek.isEmpty()) {
            return -1L;
        }

        String[] days = schedule.daysOfWeek.split(",");
        Calendar now = Calendar.getInstance();

        for (int offset = 0; offset <= 7; offset++) {
            Calendar candidate = Calendar.getInstance();
            candidate.add(Calendar.DAY_OF_YEAR, offset);
            candidate.set(Calendar.HOUR_OF_DAY, schedule.startHour);
            candidate.set(Calendar.MINUTE, schedule.startMinute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);

            if (candidate.getTimeInMillis() <= now.getTimeInMillis()) {
                continue; // skip past times
            }

            String dayAbbrv = getDayAbbreviation(candidate.get(Calendar.DAY_OF_WEEK));
            for (String d : days) {
                if (d.trim().equalsIgnoreCase(dayAbbrv)) {
                    // Alarm fires at the exact scheduled time; receiver provides the cancel window
                    return candidate.getTimeInMillis();
                }
            }
        }
        return -1L;
    }

    private PendingIntent buildPendingIntent(int scheduleId) {
        Intent intent = new Intent(context, FocusScheduleAlarmReceiver.class);
        intent.putExtra(FocusScheduleAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId);
        return PendingIntent.getBroadcast(
                context,
                scheduleId, // unique request code per schedule
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static String getDayAbbreviation(int calendarDayOfWeek) {
        return switch (calendarDayOfWeek) {
            case Calendar.MONDAY -> "MON";
            case Calendar.TUESDAY -> "TUE";
            case Calendar.WEDNESDAY -> "WED";
            case Calendar.THURSDAY -> "THU";
            case Calendar.FRIDAY -> "FRI";
            case Calendar.SATURDAY -> "SAT";
            case Calendar.SUNDAY -> "SUN";
            default -> "";
        };
    }
}
