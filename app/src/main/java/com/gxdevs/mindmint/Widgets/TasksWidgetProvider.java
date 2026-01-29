package com.gxdevs.mindmint.Widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.gxdevs.mindmint.Activities.HomeActivity;
import com.gxdevs.mindmint.Activities.WidgetQuickAddActivity;
import com.gxdevs.mindmint.Models.Task;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.TaskManager;

import java.util.concurrent.Executors;

public class TasksWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TOGGLE_TASK = "com.gxdevs.mindmint.ACTION_TOGGLE_TASK";
    public static final String ACTION_ADD_TASK = "com.gxdevs.mindmint.ACTION_ADD_TASK";
    public static final String EXTRA_TASK_ID = "task_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TasksWidgetProvider.class));
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);

        // Set up the collection
        Intent intent = new Intent(context, TasksWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list_view, intent);
        views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view);

        Intent templateIntent = new Intent(context, TasksWidgetProvider.class);
        templateIntent.setAction(ACTION_TOGGLE_TASK);
        templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        PendingIntent templatePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_list_view, templatePendingIntent);

        // Add Task Button Click - Launch transparent activity with bottom sheet
        Intent addTaskIntent = new Intent(context, WidgetQuickAddActivity.class);
        addTaskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent addTaskPendingIntent = PendingIntent.getActivity(
                context,
                1,
                addTaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_add_btn, addTaskPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TOGGLE_TASK.equals(intent.getAction())) {
            String taskId = intent.getStringExtra(EXTRA_TASK_ID);
            String cmd = intent.getStringExtra("cmd");

            if ("open".equals(cmd)) {
                Intent i = new Intent(context, HomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(i);
            } else if (taskId != null) {
                // Toggle
                Executors.newSingleThreadExecutor().execute(() -> {
                    TaskManager tm = new TaskManager(context);
                    Task t = tm.getTaskById(taskId);
                    if (t != null) {
                        t.setCompleted(!t.isCompleted());
                        tm.updateTask(t);

                        // Update widgets
                        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TasksWidgetProvider.class));
                        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view);
                    }
                });
            }
        }
        // Force update on general update actions
        else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager
                    .getAppWidgetIds(new ComponentName(context, TasksWidgetProvider.class));
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}
