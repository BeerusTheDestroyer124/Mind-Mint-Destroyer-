package com.gxdevs.mindmint.Widgets;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.gxdevs.mindmint.Models.Task;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.TaskManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context context;
    private final List<Task> items = new ArrayList<>();
    private TaskManager taskManager;

    public TasksRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        taskManager = new TaskManager(context);
    }

    @Override
    public void onDataSetChanged() {
        // This is called when we call notifyAppWidgetViewDataChanged
        items.clear();

        long identityToken = android.os.Binder.clearCallingIdentity();
        try {
            List<Task> allActive = taskManager.getActiveTasksSorted();
            int count = Math.min(allActive.size(), 5);
            for (int i = 0; i < count; i++) {
                items.add(allActive.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            android.os.Binder.restoreCallingIdentity(identityToken);
        }
    }

    @Override
    public void onDestroy() {
        items.clear();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= items.size())
            return null;
        Task task = items.get(position);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item_task);

        // Set task title
        rv.setTextViewText(R.id.widget_item_title, task.getName());

        // Set radio button state (checked/unchecked)
        int pinkColor = context.getColor(R.color.brand_pink);

        if (task.isCompleted()) {
            rv.setImageViewResource(R.id.widget_item_check, R.drawable.widget_radio_checked);
            rv.setInt(R.id.widget_item_check, "setColorFilter", pinkColor);
        } else {
            rv.setImageViewResource(R.id.widget_item_check, R.drawable.widget_radio_unchecked);
            rv.setInt(R.id.widget_item_check, "setColorFilter", pinkColor);
        }

        // Set task icon
        int iconRes = task.getIcon() != 0 ? task.getIcon() : R.drawable.list_todo;
        rv.setImageViewResource(R.id.widget_task_icon, iconRes);
        // Apply text_secondary tint to the icon
        int iconTint = context.getColor(R.color.text_secondary);
        rv.setInt(R.id.widget_task_icon, "setColorFilter", iconTint);

        // Set deadline
        if (task.getScheduledDate() != null) {
            String deadlineStr = formatDeadline(task.getScheduledDate().getTime());
            rv.setTextViewText(R.id.widget_item_deadline, deadlineStr);
            rv.setViewVisibility(R.id.widget_item_deadline, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.widget_item_deadline, View.GONE);
        }

        // Toggle Action on checkbox
        Intent toggleIntent = new Intent();
        toggleIntent.putExtra(TasksWidgetProvider.EXTRA_TASK_ID, task.getId());
        toggleIntent.putExtra("cmd", "toggle");
        rv.setOnClickFillInIntent(R.id.widget_item_check, toggleIntent);

        // Open Action on container
        Intent openIntent = new Intent();
        openIntent.putExtra(TasksWidgetProvider.EXTRA_TASK_ID, task.getId());
        openIntent.putExtra("cmd", "open");
        rv.setOnClickFillInIntent(R.id.widget_item_container, openIntent);

        return rv;
    }

    private String formatDeadline(long timestamp) {
        Calendar taskCal = Calendar.getInstance();
        taskCal.setTimeInMillis(timestamp);

        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String timeStr = timeFmt.format(taskCal.getTime());

        // Check if today
        if (isSameDay(taskCal, today)) {
            return "Today, " + timeStr;
        }
        // Check if tomorrow
        else if (isSameDay(taskCal, tomorrow)) {
            return "Tomorrow, " + timeStr;
        }
        // Check if this week
        else if (taskCal.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR) &&
                taskCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            SimpleDateFormat dayFmt = new SimpleDateFormat("EEEE", Locale.getDefault());
            return dayFmt.format(taskCal.getTime()) + ", " + timeStr;
        }
        // Otherwise show date
        else {
            SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d", Locale.getDefault());
            return dateFmt.format(taskCal.getTime()) + ", " + timeStr;
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
