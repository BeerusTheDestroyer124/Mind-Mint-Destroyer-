package com.gxdevs.mindmint.Receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.gxdevs.mindmint.Activities.HomeActivity;
import com.gxdevs.mindmint.Activities.FocusMode;
import com.gxdevs.mindmint.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles all incoming Firebase Cloud Messages for Mind Mint.
 *
 * Supported data payload keys (all sent from Firebase Console → Cloud Messaging):
 *
 *   title        – Notification title (required)
 *   body         – Notification body text (required)
 *   type         – One of: focus_reminder | feature_update | announcement | general
 *   image_url    – Optional HTTPS URL of a large image to show in the notification
 *   intent       – One of: open_home | open_focus | open_tasks | open_habits |
 *                          open_settings | open_url | check_service_status
 *   intent_value – Used when intent == "open_url" — the URL to open
 *
 * Example Firebase Console payload (Advanced options → Custom data):
 *   title        = "Time to focus! 🧠"
 *   body         = "Your next focus session is ready. Let's go!"
 *   type         = focus_reminder
 *   intent       = open_focus
 *   image_url    = https://example.com/banner.png
 */
public class FirebaseNotificationReceiver extends FirebaseMessagingService {

    private static final String TAG = "MindMintFCM";

    // ── Notification channel IDs ─────────────────────────────────────────────
    public static final String CHANNEL_FCM_GENERAL   = "fcm_general";
    public static final String CHANNEL_FCM_FOCUS      = "fcm_focus_reminders";
    public static final String CHANNEL_FCM_UPDATES    = "fcm_feature_updates";

    // ── Notification IDs ─────────────────────────────────────────────────────
    private static final int NOTIF_ID_BASE = 9000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannelsCreated(this);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Message received
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Log.d(TAG, "FCM message received from: " + message.getFrom());

        Map<String, String> data = message.getData();

        // Pull values — prefer data payload so we can always control from Firebase Console
        String title    = getValue(data, "title",    message);
        String body     = getValue(data, "body",     message);
        String type     = data.getOrDefault("type",         "general");
        String imageUrl = data.getOrDefault("image_url",    null);
        String intent   = data.getOrDefault("intent",       "open_home");
        String intentValue = data.getOrDefault("intent_value", null);

        // Special server-side action — no visual notification needed
        if ("check_service_status".equals(intent)) {
            checkAndReviveService();
            return;
        }

        if (title == null || title.isEmpty()) title = getString(R.string.app_name);
        if (body  == null || body.isEmpty())  body  = "";

        final String finalTitle      = title;
        final String finalBody       = body;
        final String finalType       = type;
        final String finalImageUrl   = imageUrl;
        final String finalIntent     = intent;
        final String finalIntentValue = intentValue;

        // Download image on a background thread, then post notification
        executor.execute(() -> {
            Bitmap bigPicture = null;
            if (finalImageUrl != null && !finalImageUrl.isEmpty()) {
                bigPicture = downloadBitmap(finalImageUrl);
            }
            showNotification(finalTitle, finalBody, finalType, bigPicture,
                    finalIntent, finalIntentValue);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Token refresh
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed: " + token);
        // If you store the token server-side, send it here.
        // For Firebase Console-only use, nothing more is needed.
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Notification builder
    // ─────────────────────────────────────────────────────────────────────────

    private void showNotification(String title, String body, String type,
                                  Bitmap bigPicture, String intentKey, String intentValue) {
        String channelId = channelForType(type);

        PendingIntent pendingIntent = buildPendingIntent(intentKey, intentValue);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.bell)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(bigPicture != null
                        ? new NotificationCompat.BigPictureStyle()
                                .bigPicture(bigPicture)
                                .bigLargeIcon((Bitmap) null)
                        : new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Large icon: use the big picture thumbnail, or the app icon
        if (bigPicture != null) {
            builder.setLargeIcon(bigPicture);
        }

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int notifId = NOTIF_ID_BASE + (int) (System.currentTimeMillis() % 1000);
            manager.notify(notifId, builder.build());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Intent routing — maps "intent" key → actual Android Intent
    // ─────────────────────────────────────────────────────────────────────────
    //
    //  COMPLETE INTENT REFERENCE
    //  ──────────────────────────────────────────────────────────────────────
    //  open_home           → Home screen (focus timer, stats overview)
    //  open_focus          → Focus Mode screen (start / resume session)
    //  open_tasks          → Tasks tab
    //  open_tasks_add      → Tasks tab + auto-opens "Add task" sheet
    //  open_habits         → Habits tab
    //  open_stats          → Stats screen (overall analytics)
    //  open_stats_focus    → Stats screen, Focus tab
    //  open_stats_habits   → Stats screen, Habits tab
    //  open_stats_tasks    → Stats screen, Tasks tab
    //  open_settings       → Settings tab
    //  open_site_blocker   → Site Blocker configuration screen
    //  open_onboarding     → Onboarding / what's new screen
    //  open_url            → External URL in browser (pass URL in intent_value)
    //  open_play_store     → App page on Google Play (for update nudges)
    //  check_service_status → (silent) Revives accessibility guard service
    // ─────────────────────────────────────────────────────────────────────────

    private PendingIntent buildPendingIntent(String intentKey, String intentValue) {
        Intent intent;

        switch (intentKey == null ? "open_home" : intentKey) {

            // ── Focus ──────────────────────────────────────────────────────
            case "open_focus":
                intent = new Intent(this, FocusMode.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            // ── Tasks ──────────────────────────────────────────────────────
            case "open_tasks":
                intent = new Intent(this, HomeActivity.class);
                intent.putExtra(HomeActivity.EXTRA_START_FRAGMENT, HomeActivity.START_FRAGMENT_TASKS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            case "open_tasks_add":
                // Opens Tasks tab and immediately shows the "Add task" bottom sheet
                intent = new Intent(this, HomeActivity.class);
                intent.putExtra(HomeActivity.EXTRA_START_FRAGMENT, HomeActivity.START_FRAGMENT_TASKS);
                intent.putExtra("open_add_task", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            // ── Habits ─────────────────────────────────────────────────────
            case "open_habits":
                intent = new Intent(this, HomeActivity.class);
                intent.putExtra(HomeActivity.EXTRA_START_FRAGMENT, HomeActivity.START_FRAGMENT_HABITS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            // ── Stats ──────────────────────────────────────────────────────
            case "open_stats":
                // Opens the main stats screen (defaults to first tab)
                intent = new Intent(this, com.gxdevs.mindmint.Activities.StatsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            case "open_stats_focus":
                intent = new Intent(this, com.gxdevs.mindmint.Activities.StatsActivity.class);
                intent.putExtra("stats_tab", "focus");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            case "open_stats_habits":
                intent = new Intent(this, com.gxdevs.mindmint.Activities.StatsActivity.class);
                intent.putExtra("stats_tab", "habits");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            case "open_stats_tasks":
                intent = new Intent(this, com.gxdevs.mindmint.Activities.StatsActivity.class);
                intent.putExtra("stats_tab", "tasks");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            // ── Settings ───────────────────────────────────────────────────
            case "open_settings":
                intent = new Intent(this, HomeActivity.class);
                intent.putExtra(HomeActivity.EXTRA_START_FRAGMENT, HomeActivity.START_FRAGMENT_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            // ── Site Blocker ───────────────────────────────────────────────
            case "open_site_blocker":
                intent = new Intent(this, com.gxdevs.mindmint.Activities.SiteBlockerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            // ── Onboarding / What's New ────────────────────────────────────
            case "open_onboarding":
                // Perfect for "new update" notifications — shows the changelog / onboarding
                intent = new Intent(this, com.gxdevs.mindmint.Activities.OnBoarding.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;

            // ── External URL ───────────────────────────────────────────────
            case "open_url":
                if (intentValue != null && !intentValue.isEmpty()) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentValue));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                } else {
                    intent = defaultHomeIntent();
                }
                break;

            // ── Play Store (for "update available" nudges) ─────────────────
            case "open_play_store":
                String pkg = getPackageName();
                try {
                    // Try the Play Store app first; falls back to browser
                    intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + pkg));
                } catch (android.content.ActivityNotFoundException e) {
                    intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + pkg));
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                break;

            // ── Home (default) ─────────────────────────────────────────────
            case "open_home":
            default:
                intent = defaultHomeIntent();
                break;
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                intent, flags);
    }

    private Intent defaultHomeIntent() {
        Intent i = new Intent(this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }


    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Resolve channel ID based on message type. */
    private String channelForType(String type) {
        if (type == null) return CHANNEL_FCM_GENERAL;
        switch (type) {
            case "focus_reminder": return CHANNEL_FCM_FOCUS;
            case "feature_update":
            case "announcement":  return CHANNEL_FCM_UPDATES;
            default:              return CHANNEL_FCM_GENERAL;
        }
    }

    /** Pulls a value preferring the data map, falling back to the notification object. */
    private String getValue(Map<String, String> data, String key, RemoteMessage message) {
        String fromData = data.get(key);
        if (fromData != null && !fromData.isEmpty()) return fromData;
        if (message.getNotification() != null) {
            if ("title".equals(key)) return message.getNotification().getTitle();
            if ("body".equals(key))  return message.getNotification().getBody();
        }
        return null;
    }

    /** Downloads a Bitmap from a URL on the calling thread. Returns null on failure. */
    private Bitmap downloadBitmap(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(input);
            input.close();
            return bmp;
        } catch (IOException e) {
            Log.e(TAG, "Failed to download notification image: " + e.getMessage());
            return null;
        }
    }

    /** Revives the guard service or prompts for accessibility — original behaviour. */
    private void checkAndReviveService() {
        if (!com.gxdevs.mindmint.Utils.Utils.isAccessibilityPermissionGranted(this)) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("from_guard", true);
            startActivity(intent);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Static helper — call once from Application.onCreate()
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates all FCM notification channels. Safe to call multiple times — Android
     * silently no-ops if the channel already exists.
     */
    public static void ensureChannelsCreated(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // General channel
        NotificationChannel general = new NotificationChannel(
                CHANNEL_FCM_GENERAL,
                "Mind Mint Notifications",
                NotificationManager.IMPORTANCE_HIGH);
        general.setDescription("General notifications from Mind Mint");

        // Focus reminders
        NotificationChannel focus = new NotificationChannel(
                CHANNEL_FCM_FOCUS,
                "Focus Reminders",
                NotificationManager.IMPORTANCE_HIGH);
        focus.setDescription("Reminders to start or stay in your focus sessions");

        // Feature updates & announcements
        NotificationChannel updates = new NotificationChannel(
                CHANNEL_FCM_UPDATES,
                "Updates & Announcements",
                NotificationManager.IMPORTANCE_DEFAULT);
        updates.setDescription("New features, updates, and announcements from the team");

        manager.createNotificationChannel(general);
        manager.createNotificationChannel(focus);
        manager.createNotificationChannel(updates);
    }
}
