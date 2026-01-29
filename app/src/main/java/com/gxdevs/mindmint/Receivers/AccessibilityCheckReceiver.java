package com.gxdevs.mindmint.Receivers;

import static com.gxdevs.mindmint.Utils.Utils.isAccessibilityPermissionGranted;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gxdevs.mindmint.Utils.AlarmUtils;
import com.gxdevs.mindmint.Utils.WarningUtils;

public class AccessibilityCheckReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isAccessibilityPermissionGranted(context)) {
            WarningUtils.showPermissionWarning(context);
        } else {
            if (WarningUtils.isWarningVisible(context)) WarningUtils.remove(context);
        }
        AlarmUtils.scheduleAlarm(context);
    }
}