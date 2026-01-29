package com.gxdevs.mindmint.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gxdevs.mindmint.Utils.MidnightResetManager;

public class MidnightResetReceiver extends BroadcastReceiver {
    private static final String TAG = "MidnightResetReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Midnight reset triggered via AlarmManager.");
        MidnightResetManager.checkAndPerformReset(context);
    }
}