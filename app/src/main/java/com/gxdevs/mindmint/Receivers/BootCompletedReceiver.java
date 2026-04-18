package com.gxdevs.mindmint.Receivers;

import static com.gxdevs.mindmint.Utils.Utils.isAccessibilityPermissionGranted;
import static com.gxdevs.mindmint.Utils.Utils.isKeepAlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gxdevs.mindmint.Services.AppUsageAccessibilityService;
import com.gxdevs.mindmint.Utils.AlarmUtils;
import com.gxdevs.mindmint.Utils.FocusScheduleManager;
import com.gxdevs.mindmint.Utils.WarningUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                new FocusScheduleManager(context).rescheduleAll();
            } catch (Exception e) {
                Log.e("BootCompletedReceiver", "Failed to reschedule focus alarms", e);
            }
            try {
                Intent refresh = new Intent(AppUsageAccessibilityService.ACTION_REFRESH_DAILY_STATE_INTERNAL);
                refresh.setPackage(context.getPackageName());
                context.sendBroadcast(refresh);
            } catch (Exception e) {
                Log.e("BootCompletedReceiver", "Failed to broadcast refresh after boot", e);
            }
            if (isKeepAlive(context)) {
                if (!isAccessibilityPermissionGranted(context)) {
                    WarningUtils.showPermissionWarning(context);
                    AlarmUtils.scheduleAlarm(context);
                }
            }
        }
    }
}


