package com.gxdevs.mindmint.Widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

import com.gxdevs.mindmint.Activities.StatsActivity;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.Utils;

public class ScreenTimeWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String actionTimeUpdated = com.gxdevs.mindmint.Common.IntentActions.getActionTimeUpdated(context);
        if (actionTimeUpdated.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ScreenTimeWidgetProvider.class));
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        int totalWastedScrolls = Utils.calculateTotalUsageScrolls(sharedPreferences, "yt") + Utils.calculateTotalUsageScrolls(sharedPreferences, "insta") + Utils.calculateTotalUsageScrolls(sharedPreferences, "snap");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_screen_time);

        // Update Count
        views.setTextViewText(R.id.widget_count, String.valueOf(totalWastedScrolls));

        // Calculate estimated time
        long estimatedTimeSeconds = sharedPreferences.getLong("total_estimated_wasted_time", 0);
        if (estimatedTimeSeconds == 0 && totalWastedScrolls > 0) {
            estimatedTimeSeconds = totalWastedScrolls * 12L;
            sharedPreferences.edit().putLong("total_estimated_wasted_time", estimatedTimeSeconds).apply();
        }

        String timeText = formatTime(estimatedTimeSeconds);
        views.setTextViewText(R.id.widget_wasted_time, "~ " + timeText);

        // Update Icon based on count
        int drawableId = getDrawableId(totalWastedScrolls);
        views.setImageViewResource(R.id.widget_icon, drawableId);

        // Click Intent -> StatsActivity
        Intent intent = new Intent(context, StatsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static int getDrawableId(int totalWastedScrolls) {
        int drawableId;
        if (totalWastedScrolls < 150) {
            drawableId = R.drawable.brain1;
        } else if (totalWastedScrolls < 300) {
            drawableId = R.drawable.brain2;
        } else if (totalWastedScrolls < 500) {
            drawableId = R.drawable.brain3;
        } else if (totalWastedScrolls < 700) {
            drawableId = R.drawable.brain4;
        } else if (totalWastedScrolls < 900) {
            drawableId = R.drawable.brain5;
        } else if (totalWastedScrolls < 1100) {
            drawableId = R.drawable.brain6;
        } else if (totalWastedScrolls < 1200) {
            drawableId = R.drawable.brain7;
        } else if (totalWastedScrolls < 1400) {
            drawableId = R.drawable.brain8;
        } else {
            drawableId = R.drawable.brain9;
        }
        return drawableId;
    }

    private static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m";
    }
}
