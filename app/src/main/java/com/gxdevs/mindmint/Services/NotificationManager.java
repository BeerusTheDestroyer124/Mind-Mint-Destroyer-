package com.gxdevs.mindmint.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.gxdevs.mindmint.Activities.HomeActivity;
import com.gxdevs.mindmint.R;

/**
 * NotificationManager - Handles all push notifications for focus sessions and reminders
 */
public class NotificationService {
    
    private static final String FOCUS_CHANNEL_ID = "focus_session_channel";
    private static final String REWARD_CHANNEL_ID = "reward_notification_channel";
    private static final int FOCUS_REMINDER_ID = 101;
    private static final int REWARD_NOTIFICATION_ID = 102;
    
    private final Context context;
    private final NotificationManager notificationManager;
    
    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }
    
    /**
     * Create notification channels for Android 8.0+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Focus Session Channel
            NotificationChannel focusChannel = new NotificationChannel(
                    FOCUS_CHANNEL_ID,
                    "Focus Session Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            focusChannel.setDescription("Notifications for focus session reminders and updates");
            focusChannel.enableVibration(true);
            focusChannel.setShowBadge(true);
            
            // Reward Channel
            NotificationChannel rewardChannel = new NotificationChannel(
                    REWARD_CHANNEL_ID,
                    "Crystal Rewards",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            rewardChannel.setDescription("Notifications for crystal rewards and achievements");
            rewardChannel.enableVibration(true);
            rewardChannel.setShowBadge(true);
            
            notificationManager.createNotificationChannel(focusChannel);
            notificationManager.createNotificationChannel(rewardChannel);
        }
    }
    
    /**
     * Send focus session reminder notification
     * @param focusSessionMinutes Duration of focus session in minutes
     * @param appName Name of the app being blocked
     */
    public void sendFocusReminderNotification(int focusSessionMinutes, String appName) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FOCUS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Focus Session Active")
                .setContentText("Stay focused for " + focusSessionMinutes + " minutes. " + appName + " is blocked.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your focus session is active. " + appName + " has been blocked to help you concentrate for " + focusSessionMinutes + " minutes."));
        
        notificationManager.notify(FOCUS_REMINDER_ID, builder.build());
    }
    
    /**
     * Send focus session completion notification
     * @param minutesFocused Total minutes focused
     */
    public void sendFocusCompletionNotification(int minutesFocused) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                1, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FOCUS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Focus Session Complete!")
                .setContentText("Great job! You focused for " + minutesFocused + " minutes.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Congratulations! You have successfully completed your " + minutesFocused + " minute focus session."));
        
        notificationManager.notify(FOCUS_REMINDER_ID, builder.build());
    }
    
    /**
     * Send mid-session reminder notification
     * @param remainingMinutes Minutes remaining in focus session
     */
    public void sendMidSessionReminder(int remainingMinutes) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                2, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FOCUS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Keep Going!")
                .setContentText(remainingMinutes + " minutes left in your focus session")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        notificationManager.notify(FOCUS_REMINDER_ID + remainingMinutes, builder.build());
    }
    
    /**
     * Cancel focus session notification
     */
    public void cancelFocusNotification() {
        notificationManager.cancel(FOCUS_REMINDER_ID);
    }
}
