package com.gxdevs.mindmint.Receivers;

import static com.gxdevs.mindmint.Utils.Utils.isAccessibilityPermissionGranted;
import static com.gxdevs.mindmint.Utils.Utils.isKeepAlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gxdevs.mindmint.Services.AppUsageAccessibilityService;
import com.gxdevs.mindmint.Utils.AlarmUtils;
import com.gxdevs.mindmint.Utils.WarningUtils;

public class NotificationDismissBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = isAccessibilityPermissionGranted(context);
        if (!isKeepAlive(context)) return;
        Intent restoreIntent = new Intent(AppUsageAccessibilityService.RESTORE_NOTIFICATION);
        restoreIntent.setPackage(context.getPackageName());
        context.sendBroadcast(restoreIntent);
        if (enabled) {
            if (!AppUsageAccessibilityService.serviceStatus) {
                WarningUtils.showPermissionWarning(context);
            }
        } else {
            WarningUtils.showPermissionWarning(context);
            AlarmUtils.scheduleAlarm(context);
        }
    }
}