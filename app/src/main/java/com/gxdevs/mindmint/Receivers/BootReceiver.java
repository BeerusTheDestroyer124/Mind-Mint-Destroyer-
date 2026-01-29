package com.gxdevs.mindmint.Receivers;

import static com.gxdevs.mindmint.Utils.Utils.isAccessibilityPermissionGranted;
import static com.gxdevs.mindmint.Utils.Utils.isKeepAlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gxdevs.mindmint.Utils.AlarmUtils;
import com.gxdevs.mindmint.Utils.WarningUtils;

import java.util.Objects;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (isKeepAlive(context) && Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            if (!isAccessibilityPermissionGranted(context)) {
                WarningUtils.showPermissionWarning(context);
                AlarmUtils.scheduleAlarm(context);
            }
        }
    }
}


