package com.gxdevs.mindmint.Utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import com.gxdevs.mindmint.Models.Task;
import com.gxdevs.mindmint.Widgets.TasksWidgetProvider;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.TaskDao;
import com.gxdevs.mindmint.db.entities.TaskEntity;
import com.gxdevs.mindmint.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class TaskManager {
    private static final String PREFS_NAME = "TaskPrefs";
    private static boolean oldPrefsCleared = false;

    private final TaskDao taskDao;
    private final Context context;

    public TaskManager(Context context) {
        this.context = context;
        this.taskDao = MindMintRoomDatabase.getInstance(context).taskDao();

        if (!oldPrefsCleared) {
            clearOldSharedPreferences(context);
            oldPrefsCleared = true;
        }
    }


    private static void clearOldSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public void saveTasks(List<Task> tasks) {
        taskDao.deleteAll();
        List<TaskEntity> entities = new ArrayList<>();
        for (Task t : tasks)
            entities.add(toTaskEntity(t));
        taskDao.insertAll(entities);
    }

    public List<Task> loadTasks() {
        List<TaskEntity> entities = taskDao.getAll();
        List<Task> out = new ArrayList<>();
        for (TaskEntity e : entities)
            out.add(fromTaskEntity(e));
        return out;
    }

    public List<Task> getTasksSortedByPriorityAndTime() {
        List<Task> allTasks = loadTasks();
        List<Task> incompleteTasks = new ArrayList<>();
        for (Task t : allTasks) {
            if (!t.isCompleted()) {
                incompleteTasks.add(t);
            }
        }

        incompleteTasks.sort((t1, t2) -> {
            int p1 = t1.getPriority() != null ? t1.getPriority().getValue() : 2;
            int p2 = t2.getPriority() != null ? t2.getPriority().getValue() : 2;
            if (p1 != p2)
                return Integer.compare(p1, p2);

            Date d1 = t1.getScheduledDate();
            Date d2 = t2.getScheduledDate();
            if (d1 == null && d2 == null)
                return 0;
            if (d1 == null)
                return 1; // No date comes last
            if (d2 == null)
                return -1;
            return d1.compareTo(d2);
        });

        return incompleteTasks;
    }

    public void addTask(Task task) {
        taskDao.insert(toTaskEntity(task));
        updateWidgets();
    }

    public void updateTask(Task updatedTask) {
        taskDao.update(toTaskEntity(updatedTask));

        updateDailyStats();
        updateWidgets();
    }

    private void updateDailyStats() {
        try {
            StatsManager statsManager = new StatsManager(context);
            statsManager.updateTodayStats();
        } catch (Exception e) {
            // Context might not be available in some cases, ignore
        }
    }

    public void deleteTask(String taskId) {
        taskDao.deleteById(taskId);
        updateWidgets();
    }


    private void updateWidgets() {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, TasksWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, com.gxdevs.mindmint.R.id.widget_list_view);
        } catch (Exception e) {
            // Ignore if widget not found or other errors
        }
    }

    private TaskEntity toTaskEntity(Task t) {
        TaskEntity e = new TaskEntity();
        e.id = t.getId();
        e.name = t.getName();
        e.short_description = t.getShortDescription();
        e.image_resource = t.getImageResource();
        e.is_completed = t.isCompleted() ? 1 : 0;
        e.priority_value = t.getPriority() != null ? t.getPriority().getValue() : 2;
        e.created_date_ms = toMs(t.getCreatedDate());
        e.completed_date_ms = toMs(t.getCompletedDate());
        e.scheduled_date_ms = toMs(t.getScheduledDate());
        e.recurring_type = t.getRecurringType() != null ? t.getRecurringType().name() : "NONE";
        e.is_recurring = t.isRecurring() ? 1 : 0;
        e.has_reminder = t.hasReminder() ? 1 : 0;
        e.repeat_options_json = t.getRepeatOptionsJson();
        e.is_habit = t.isHabit() ? 1 : 0;
        e.habit_id = t.getHabitId();

        int iconRes = t.getIcon();
        if (iconRes == 0) {
            iconRes = R.drawable.list_todo;
        }
        try {
            e.icon_name = context.getResources().getResourceEntryName(iconRes);
        } catch (Exception ignored) {
            e.icon_name = "todo";
        }

        if (t.isCompleted() && e.completed_date_ms > 0) {
            try {
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", java.util.Locale.US);
                Date completedDate = new Date(e.completed_date_ms);
                e.completed_date_str = sdfDate.format(completedDate);
                e.completed_time_str = sdfTime.format(completedDate);
            } catch (Exception ex) {
                // Ignore formatting errors
            }
        } else {
            e.completed_date_str = null;
            e.completed_time_str = null;
        }

        // Focus mode fields
        e.focus_mode_enabled = t.isFocusModeEnabled() ? 1 : 0;
        e.focus_duration_minutes = t.getFocusDurationMinutes();
        e.focus_time_spent_ms = t.getFocusTimeSpentMs();
        e.focus_status = t.getFocusStatus();

        return e;
    }

    private Task fromTaskEntity(TaskEntity e) {
        Task t = new Task();
        t.setId(e.id);
        t.setName(e.name);
        t.setShortDescription(e.short_description);
        t.setImageResource(e.image_resource);
        t.setCompleted(e.is_completed == 1);
        t.setPriority(fromPriorityValue(e.priority_value));
        t.setCreatedDate(fromMs(e.created_date_ms));
        t.setCompletedDate(e.completed_date_ms > 0 ? new Date(e.completed_date_ms) : null);
        t.setScheduledDate(e.scheduled_date_ms > 0 ? new Date(e.scheduled_date_ms) : null);
        String recurringName = e.recurring_type;
        try {
            t.setRecurringType(Task.RecurringType.valueOf(recurringName != null ? recurringName : "NONE"));
        } catch (Exception ex) {
            t.setRecurringType(Task.RecurringType.NONE);
        }
        t.setRecurring(e.is_recurring == 1);
        t.setHasReminder(e.has_reminder == 1);
        t.setRepeatOptionsJson(e.repeat_options_json);
        t.setHabit(e.is_habit == 1);
        t.setHabitId(e.habit_id);

        // Resolve drawable from stored name; fall back safely if missing
        int iconRes = 0;
        if (e.icon_name != null && !e.icon_name.isEmpty()) {
            iconRes = context.getResources().getIdentifier(e.icon_name, "drawable", context.getPackageName());
        }
        if (iconRes == 0) {
            iconRes = R.drawable.list_todo;
        }
        t.setIcon(iconRes);

        // Focus mode fields
        t.setFocusModeEnabled(e.focus_mode_enabled == 1);
        t.setFocusDurationMinutes(e.focus_duration_minutes);
        t.setFocusTimeSpentMs(e.focus_time_spent_ms);
        t.setFocusStatus(e.focus_status != null ? e.focus_status : "IDLE");

        return t;
    }

    private long toMs(Date d) {
        return d != null ? d.getTime() : 0L;
    }

    private Date fromMs(long ms) {
        return ms > 0 ? new Date(ms) : null;
    }

    private Task.Priority fromPriorityValue(int v) {
        for (Task.Priority p : Task.Priority.values()) {
            if (p.getValue() == v)
                return p;
        }
        return Task.Priority.MEDIUM;
    }

    public List<Task> getActiveTasksSorted() {
        List<Task> all = loadTasks();
        List<Task> active = new ArrayList<>();
        for (Task t : all) {
            if (!t.isCompleted()) {
                active.add(t);
            }
        }

        active.sort((t1, t2) -> {
            // Priority Value DESC (High=3 > Medium=2 > Low=1)
            int p1 = t1.getPriority() != null ? t1.getPriority().getValue() : 1;
            int p2 = t2.getPriority() != null ? t2.getPriority().getValue() : 1;
            int cmp = Integer.compare(p2, p1);
            if (cmp != 0)
                return cmp;

            // Date ASC (Earlier before Later)
            Date d1 = t1.getScheduledDate();
            Date d2 = t2.getScheduledDate();

            if (d1 == null && d2 == null)
                return 0;
            if (d1 == null)
                return 1; // No date -> End
            if (d2 == null)
                return -1;

            return d1.compareTo(d2);
        });

        return active;
    }

    public Task getTaskById(String id) {
        List<Task> all = loadTasks();
        for (Task t : all) {
            if (t.getId().equals(id))
                return t;
        }
        return null;
    }
}
