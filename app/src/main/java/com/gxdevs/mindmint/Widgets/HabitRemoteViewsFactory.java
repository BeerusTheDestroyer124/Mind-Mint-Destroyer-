package com.gxdevs.mindmint.Widgets;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.HabitManager;
import com.gxdevs.mindmint.Utils.WidgetBitmapUtils;

import java.util.ArrayList;
import java.util.List;

public class HabitRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private List<Habit> habitList = new ArrayList<>();
    private final HabitManager habitManager;

    public HabitRemoteViewsFactory(Context context) {
        this.context = context;
        habitManager = new HabitManager(context);
    }

    @Override
    public void onCreate() {
        // Init logic
    }

    @Override
    public void onDataSetChanged() {
        long identityToken = Binder.clearCallingIdentity();
        try {
            habitList = habitManager.loadHabits();
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    @Override
    public void onDestroy() {
        habitList.clear();
    }

    @Override
    public int getCount() {
        return habitList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= habitList.size())
            return null;

        try {
            Habit habit = habitList.get(position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_item_habit);

            views.setTextViewText(R.id.widget_habit_title, habit.getName());
            int iconRes = habit.getIcon() != 0 ? habit.getIcon() : R.drawable.flame;
            boolean isCompleted = habitManager.isCompletedToday(habit);
            boolean isGoal = habit.isGoalTracking();
            int tintColor = habit.getIconTint();
            int bgColor = habit.getIconBackgroundTint();
            if (tintColor == 0) tintColor = context.getColor(R.color.widget_text_primary);
            if (bgColor == 0) bgColor = context.getColor(R.color.brand_pink_subtle);
            int white = context.getColor(android.R.color.white);
            int translucentWhite = 0x4DFFFFFF;
            int primaryTextColor = context.getColor(R.color.widget_text_primary);

            if (isGoal) {
                habit.resetProgressIfNeeded();
                views.setViewVisibility(R.id.widget_layout_progress_info, View.VISIBLE);
                views.setViewVisibility(R.id.widget_tv_mark, View.GONE);

                String progressText = habit.getCurrentProgress() + "/" + habit.getTargetCount();
                views.setTextViewText(R.id.widget_tv_progress_text, progressText);

                int progressLevel = 0;
                if (habit.getTargetCount() > 0) {
                    progressLevel = (int) ((habit.getCurrentProgress() / (float) habit.getTargetCount()) * 10000);
                }

                if (isCompleted) {
                    views.setTextViewText(R.id.widget_tv_progress_text, "Completed");
                    views.setTextColor(R.id.widget_tv_progress_text, white);
                    Bitmap iconBitmap = WidgetBitmapUtils.createHabitIconBitmap(context, iconRes, white, translucentWhite);
                    views.setImageViewBitmap(R.id.widget_habit_icon, iconBitmap);
                    views.setTextColor(R.id.widget_habit_title, white);
                    views.setInt(R.id.widget_habit_item_container, "setBackgroundResource", R.drawable.bg_brand_gradient);
                    views.setInt(R.id.widget_pb_bg, "setColorFilter", translucentWhite);
                    views.setInt(R.id.widget_pb_progress, "setColorFilter", white);
                    views.setInt(R.id.widget_pb_progress, "setImageLevel", 10000); // Full when completed

                } else {
                    views.setTextColor(R.id.widget_tv_progress_text, tintColor);
                    Bitmap iconBitmap = WidgetBitmapUtils.createHabitIconBitmap(context, iconRes, tintColor, bgColor);
                    views.setImageViewBitmap(R.id.widget_habit_icon, iconBitmap);
                    views.setTextColor(R.id.widget_habit_title, primaryTextColor);
                    views.setInt(R.id.widget_habit_item_container, "setBackgroundResource", R.drawable.bg_widget_card);
                    views.setInt(R.id.widget_pb_bg, "setColorFilter", bgColor);
                    views.setInt(R.id.widget_pb_progress, "setColorFilter", tintColor);
                    views.setInt(R.id.widget_pb_progress, "setImageLevel", progressLevel);
                }
            } else {
                // NORMAL HABIT
                views.setViewVisibility(R.id.widget_layout_progress_info, View.GONE);
                views.setViewVisibility(R.id.widget_tv_mark, View.VISIBLE); // Fix: Ensure visible if recycled

                if (isCompleted) {
                    views.setTextViewText(R.id.widget_tv_mark, "COMPLETED");
                    views.setTextColor(R.id.widget_tv_mark, white);
                    Bitmap iconBitmap = WidgetBitmapUtils.createHabitIconBitmap(context, iconRes, white, translucentWhite);
                    views.setImageViewBitmap(R.id.widget_habit_icon, iconBitmap);
                    views.setTextColor(R.id.widget_habit_title, white);
                    views.setInt(R.id.widget_habit_item_container, "setBackgroundResource",
                            R.drawable.bg_brand_gradient);
                } else {
                    views.setTextViewText(R.id.widget_tv_mark, "CHECK IN");
                    views.setTextColor(R.id.widget_tv_mark, context.getColor(R.color.widget_text_tertiary));
                    Bitmap iconBitmap = WidgetBitmapUtils.createHabitIconBitmap(context, iconRes, tintColor, bgColor);
                    views.setImageViewBitmap(R.id.widget_habit_icon, iconBitmap);
                    views.setTextColor(R.id.widget_habit_title, primaryTextColor);
                    views.setInt(R.id.widget_habit_item_container, "setBackgroundResource", R.drawable.bg_widget_card);
                }
            }
            Intent fillInIntent = new Intent();
            fillInIntent.putExtra(HabitListWidgetProvider.EXTRA_HABIT_ID, habit.getId());
            fillInIntent.putExtra(HabitListWidgetProvider.EXTRA_ACTION_TYPE, HabitListWidgetProvider.ACTION_TYPE_TOGGLE);
            views.setOnClickFillInIntent(R.id.widget_habit_item_container, fillInIntent);

            return views;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
        return true;
    }
}
