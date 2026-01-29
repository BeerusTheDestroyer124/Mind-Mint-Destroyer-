package com.gxdevs.mindmint.Widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.gxdevs.mindmint.Activities.HabitCreateActivity;
import com.gxdevs.mindmint.Activities.WidgetActionActivity;
import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.HabitManager;
import com.gxdevs.mindmint.Utils.MintCrystals;
import com.gxdevs.mindmint.Utils.StreakManager;

public class HabitListWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_WIDGET_CLICK = "com.gxdevs.mindmint.ACTION_WIDGET_CLICK";
    public static final String EXTRA_HABIT_ID = "com.gxdevs.mindmint.EXTRA_HABIT_ID";
    public static final String EXTRA_ACTION_TYPE = "com.gxdevs.mindmint.EXTRA_ACTION_TYPE";

    public static final String ACTION_TYPE_TOGGLE = "toggle";
    public static final String ACTION_TYPE_INCREMENT = "increment";
    public static final String ACTION_TYPE_DECREMENT = "decrement";
    public static final String ACTION_TYPE_OPEN = "open";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_habits_list);

        Intent addIntent = new Intent(context, HabitCreateActivity.class);
        PendingIntent addPendingIntent = PendingIntent.getActivity(context, 0, addIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_add_habit_btn, addPendingIntent);

        Intent serviceIntent = new Intent(context, HabitListService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_habit_list_view, serviceIntent);
        views.setEmptyView(R.id.widget_habit_list_view, R.id.widget_empty_view);

        Intent toastIntent = new Intent(context, HabitListWidgetProvider.class);
        toastIntent.setAction(ACTION_WIDGET_CLICK);
        toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_habit_list_view, toastPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_WIDGET_CLICK.equals(intent.getAction())) {
            String habitId = intent.getStringExtra(EXTRA_HABIT_ID);
            String actionType = intent.getStringExtra(EXTRA_ACTION_TYPE);

            if (habitId != null) {
                if (actionType != null) {
                    handleAction(context, habitId, actionType);
                } else {
                    // Fallback to old toggle behavior or open
                    handleAction(context, habitId, ACTION_TYPE_OPEN);
                }
            }
        }
    }

    private void handleAction(Context context, String habitId, String actionType) {
        HabitManager habitManager = new HabitManager(context);
        Habit habit = habitManager.getHabitById(habitId);

        if (habit == null)
            return;

        boolean completedToday = habitManager.isCompletedToday(habit);
        boolean updateWidgets = false;

        switch (actionType) {
            case ACTION_TYPE_INCREMENT:
                habit.resetProgressIfNeeded();
                if (habit.getCurrentProgress() < habit.getTargetCount()) {
                    int newP = habit.getCurrentProgress() + habit.getOneTapValue();
                    int finalP = Math.min(newP, habit.getTargetCount());
                    habit.setCurrentProgress(finalP);
                    habit.setLastProgressDateMs(System.currentTimeMillis());

                    boolean justComp = habitManager.updateHabitProgress(habit, finalP);

                    if (justComp) {
                        new MintCrystals(context).addCoins(5);
                        new StreakManager(context).updateStreakOnHabitCompletion();
                        // Check emotion
                        if (habit.isAskEmotion()) {
                            // If we want to show emotion dialog, we must open activity
                            openActivity(context, habitId, "MOOD");
                        }
                    }
                    updateWidgets = true;
                }
                break;

            case ACTION_TYPE_DECREMENT:
                // Allow decrementing even if completedToday is true!
                habit.resetProgressIfNeeded();
                if (habit.getCurrentProgress() > 0) {
                    boolean wasCompleted = habitManager.isCompletedToday(habit);
                    int newP = Math.max(0, habit.getCurrentProgress() - habit.getOneTapValue());

                    // Update DB with new progress
                    habit.setCurrentProgress(newP);
                    habit.setLastProgressDateMs(System.currentTimeMillis());
                    habitManager.updateHabitProgress(habit, newP);

                    if (wasCompleted && newP < habit.getTargetCount()) {
                        new MintCrystals(context).subtractCoins(5);
                        new StreakManager(context).checkAndResetStreakIfNeeded(java.util.Collections.singletonList(habit));
                    }
                    updateWidgets = true;
                }
                break;

            case ACTION_TYPE_TOGGLE:
                if (habit.isGoalTracking()) {
                    openActivity(context, habitId, "GOAL");
                } else {
                    if (completedToday) {
                        habitManager.unmarkHabit(habit);
                        new MintCrystals(context).subtractCoins(5);
                        new StreakManager(context)
                                .checkAndResetStreakIfNeeded(java.util.Collections.singletonList(habit));
                        updateWidgets = true;
                    } else {
                        if (habit.isAskEmotion()) {
                            openActivity(context, habitId, "MOOD");
                        } else {
                            completeHabit(context, habitManager, habit);
                            updateWidgets = true;
                        }
                    }
                }
                break;

            case ACTION_TYPE_OPEN:
            default:
                if (habit.isGoalTracking()) {
                    openActivity(context, habitId, "GOAL");
                } else if (habit.isAskEmotion() && !completedToday) {
                    openActivity(context, habitId, "GOAL");
                } else {
                    handleAction(context, habitId, ACTION_TYPE_TOGGLE);
                    return;
                }
                break;
        }

        if (updateWidgets) {
            updateWidgets(context);
        }
    }

    private void openActivity(Context context, String habitId, String mode) {
        Intent actionIntent = new Intent(context, WidgetActionActivity.class);
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear task to ensure
                                                                                                // fresh dialog
        actionIntent.putExtra("habit_id", habitId);
        actionIntent.putExtra("mode", mode);
        context.startActivity(actionIntent);
    }

    private void completeHabit(Context context, HabitManager hm, Habit habit) {
        hm.markHabit(habit, null);
        new MintCrystals(context).addCoins(5);
        new StreakManager(context).updateStreakOnHabitCompletion();
    }

    private void updateWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, HabitListWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_habit_list_view);
    }
}
