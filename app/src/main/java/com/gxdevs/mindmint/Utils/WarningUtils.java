package com.gxdevs.mindmint.Utils;

import static com.gxdevs.mindmint.Utils.Utils.isKeepAlive;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.gxdevs.mindmint.Activities.HomeActivity;
import com.gxdevs.mindmint.R;

public class WarningUtils {

    public static final String CHANNEL_ID = "permission_warning";
    public static final int NOTIFICATION_ID = 4401;

    public static boolean isWarningVisible(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (sbn.getId() == NOTIFICATION_ID && CHANNEL_ID.equals(sbn.getNotification().getChannelId())) {
                    return true;
                }
            }
        } catch (Exception exception) {
            Toast.makeText(context, "IDK what happened", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public static void showPermissionWarning(Context context) {
        boolean isAlive = isKeepAlive(context);
        if (!isAlive) return;
        if (isWarningVisible(context)) return;

        createChannel(context);

        Intent open = new Intent(context, HomeActivity.class);
        open.putExtra("from_guard", true);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder n = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_report)
                .setContentTitle("Enable MindMint Accessibility")
                .setContentText("Permission disabled • Tap to enable")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .addAction(R.drawable.settings, "Fix now", pi)
                .setVibrate(new long[]{0, 200})
                .setContentIntent(pi);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    return; // do not crash
                }
            }
            nm.notify(NOTIFICATION_ID, n.build());
        } catch (Exception ignored) {
        }
    }

    public static void remove(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    private static void createChannel(Context context) {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                "MindMint Accessibility Warning",
                NotificationManager.IMPORTANCE_HIGH
        );
        ch.enableVibration(true);
        ch.enableLights(false);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(ch);
    }
}