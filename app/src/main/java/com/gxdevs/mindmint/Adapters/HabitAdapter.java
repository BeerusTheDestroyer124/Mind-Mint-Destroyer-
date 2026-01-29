package com.gxdevs.mindmint.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.mindmint.Activities.HabitStatActivity;
import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.HabitManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    public interface OnHabitActionListener {
        void onHabitCompletedToday(Habit habit, int position);

        void onHabitClicked(Habit habit, int position);

        void onHabitLongPressed(Habit habit, int position);

        void onHabitUncompletedToday(Habit habit, int position);

        void onShowEmotionLog(Habit habit);

        void onEditHabit(Habit habit);

        void onDeleteHabit(Habit habit);

        void onHabitProgressUpdated(Habit habit);
    }

    private final Context context;
    private final List<Habit> habits;
    private final OnHabitActionListener listener;
    private final HabitManager habitManager;

    // Theme Colors
    private final int textPrimary;
    private final int textSecondary;
    private final int brandColor;

    public HabitAdapter(Context context, List<Habit> habits, OnHabitActionListener listener) {
        this.context = context;
        this.habits = new ArrayList<>(habits);
        this.listener = listener;
        this.habitManager = new HabitManager(context);
        this.textPrimary = getThemeColor(context, R.attr.text_primary);
        this.textSecondary = getThemeColor(context, R.attr.text_secondary);
        this.brandColor = getThemeColor(context, R.attr.brand_pink);
    }

    private int getThemeColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habits.get(position);
        boolean isCompleted = habitManager.isCompletedToday(habit);
        boolean isGoal = habit.isGoalTracking();

        holder.title.setText(habit.getName());
        holder.reason.setText(habit.getReason());
        holder.icon.setImageResource(habit.getIcon() != 0 ? habit.getIcon() : R.drawable.flame);

        // Menu
        holder.ivMore.setOnClickListener(v -> showPopupMenu(v, habit));

        // Setup Gradient Visibility
        if (isCompleted) {
            holder.gradientView.setVisibility(View.VISIBLE);
            holder.gradientView.setAlpha(1f);
            holder.gradientView.setScaleY(1f);
            holder.gradientView.post(() -> holder.gradientView.setPivotY(holder.gradientView.getHeight()));
        } else {
            holder.gradientView.setVisibility(View.GONE);
        }

        // Apply Colors based on current state
        if (isGoal) {
            setupGoalHabit(holder, habit, isCompleted);
        } else {
            setupNormalHabit(holder, habit, isCompleted);
        }

        // Long press
        holder.itemView.setOnLongClickListener(v -> {
            listener.onEditHabit(habit);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            context.startActivity(new Intent(context, HabitStatActivity.class).putExtra("habit_id", habit.getId()));
        });
    }

    private void setupNormalHabit(HabitViewHolder holder, Habit habit, boolean isCompleted) {
        holder.reason.setVisibility(View.VISIBLE);
        holder.layoutProgressInfo.setVisibility(View.INVISIBLE);
        holder.btnMinus.setVisibility(View.GONE);
        holder.btnPlus.setVisibility(View.GONE);
        holder.divider.setVisibility(View.GONE);
        holder.tvMark.setVisibility(View.VISIBLE);

        // Reset Background Drawable
        holder.actionContainer.setBackgroundResource(R.drawable.habit_action_btn);

        // Logic
        holder.tvMark.setText(isCompleted ? "COMPLETED" : "CHECK IN");
        View.OnClickListener action = v -> {
            if (isCompleted) {
                habitManager.unmarkHabit(habit);
                listener.onHabitUncompletedToday(habit, holder.getAdapterPosition());
            } else {
                habitManager.markHabit(habit);
                listener.onHabitCompletedToday(habit, holder.getAdapterPosition());
            }
            // Refresh item to update colors instantly
            notifyItemChanged(holder.getAdapterPosition());
        };
        holder.tvMark.setOnClickListener(action);
        holder.actionContainer.setOnClickListener(action);

        // --- COLORS ---
        if (!isCompleted) {
            // Normal Unmarked
            int iconTint = habit.getIconTint();
            int iconBg = habit.getIconBackgroundTint();

            holder.icon.setColorFilter(iconTint);
            holder.icon.setBackgroundTintList(ColorStateList.valueOf(iconBg));
            holder.title.setTextColor(textPrimary);
            holder.reason.setTextColor(textPrimary);
            holder.tvMark.setTextColor(textPrimary);
            holder.actionContainer.setBackgroundTintList(null); // No special tint
            holder.actionContainer.setBackgroundResource(R.drawable.habit_action_btn);
            holder.ivMore.setImageTintList(ColorStateList.valueOf(textPrimary));
        } else {
            // Normal Marked
            holder.icon.setColorFilter(Color.WHITE);
            holder.icon.setBackgroundTintList(ColorStateList.valueOf(0x4DFFFFFF));
            holder.title.setTextColor(Color.WHITE);
            holder.reason.setTextColor(Color.WHITE);

            GradientDrawable markedBg = new GradientDrawable();
            markedBg.setShape(GradientDrawable.RECTANGLE);
            markedBg.setCornerRadius(14 * context.getResources().getDisplayMetrics().density);
            markedBg.setColor(Color.WHITE);
            holder.actionContainer.setBackground(markedBg);
            holder.actionContainer.setBackgroundTintList(null);

            holder.tvMark.setTextColor(brandColor); // Completed text in brand color
            holder.ivMore.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
    }

    private void setupGoalHabit(HabitViewHolder holder, Habit habit, boolean isCompleted) {
        // UI Structure
        holder.reason.setVisibility(View.GONE);
        holder.layoutProgressInfo.setVisibility(View.VISIBLE);
        holder.btnMinus.setVisibility(View.VISIBLE);
        holder.btnPlus.setVisibility(View.VISIBLE);
        holder.divider.setVisibility(View.VISIBLE);
        holder.tvMark.setVisibility(View.GONE);

        // Reset Background Drawable
        holder.actionContainer.setBackgroundResource(R.drawable.habit_action_btn);

        // Logic
        holder.tvCurrentProgress.setText(String.valueOf(habit.getCurrentProgress()));
        holder.tvTarget.setText(habit.getTargetCount() + " " + (habit.getTargetUnit() != null ? habit.getTargetUnit() : ""));
        holder.pbGoal.setMax(habit.getTargetCount());
        holder.pbGoal.setProgress(habit.getCurrentProgress());

        holder.btnPlus.setOnClickListener(v -> {
            if (habitManager.isCompletedToday(habit)) {
                return; // Already at goal, ignore
            }
            int newP = habit.getCurrentProgress() + habit.getOneTapValue();
            boolean justComp = habitManager.updateHabitProgress(habit, newP);
            habit.setCurrentProgress(Math.min(newP, habit.getTargetCount()));
            if (justComp && habit.isAskEmotion()) listener.onShowEmotionLog(habit);
            listener.onHabitProgressUpdated(habit);
            notifyItemChanged(holder.getAdapterPosition());
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (habit.getCurrentProgress() > 0) {
                int newP = Math.max(0, habit.getCurrentProgress() - habit.getOneTapValue());
                habitManager.updateHabitProgress(habit, newP);
                habit.setCurrentProgress(newP);
                listener.onHabitProgressUpdated(habit);
                notifyItemChanged(holder.getAdapterPosition());
            }
        });

        // Remove container click for goals (buttons handle it)
        holder.actionContainer.setOnClickListener(null);

        // --- COLORS ---
        if (!isCompleted) {
            int savedColor = habit.getIconTint();
            int savedBg = habit.getIconBackgroundTint();

            holder.icon.setColorFilter(savedColor);
            holder.icon.setBackgroundTintList(ColorStateList.valueOf(savedBg));
            holder.title.setTextColor(textPrimary);
            holder.reason.setTextColor(textPrimary);
            holder.actionContainer.setBackgroundTintList(null);
            holder.actionContainer.setBackgroundResource(R.drawable.habit_action_btn);
            holder.btnPlus.setImageTintList(ColorStateList.valueOf(brandColor));
            holder.btnMinus.setImageTintList(ColorStateList.valueOf(brandColor));
            holder.pbGoal.setProgressTintList(ColorStateList.valueOf(savedColor));
            holder.pbGoal.setProgressBackgroundTintList(ColorStateList.valueOf(savedBg));
            holder.tvCurrentProgress.setTextColor(savedColor);
            holder.tvTarget.setTextColor(textSecondary);
            holder.ivMore.setImageTintList(ColorStateList.valueOf(textPrimary));

        } else {
            // Goal Marked
            holder.icon.setColorFilter(Color.WHITE);
            holder.icon.setBackgroundTintList(ColorStateList.valueOf(0x4DFFFFFF));
            holder.title.setTextColor(Color.WHITE);
            holder.reason.setTextColor(Color.WHITE);

            // Force Translucent White Background for Marked State
            GradientDrawable markedBg = new GradientDrawable();
            markedBg.setShape(GradientDrawable.RECTANGLE);
            markedBg.setCornerRadius(14 * context.getResources().getDisplayMetrics().density);
            markedBg.setColor(0x4DFFFFFF);
            holder.actionContainer.setBackground(markedBg);
            holder.actionContainer.setBackgroundTintList(null);
            holder.btnPlus.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            holder.btnMinus.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            holder.pbGoal.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
            holder.pbGoal.setProgress(habit.getTargetCount()); // Ensure full bar
            holder.tvCurrentProgress.setText(""); // Hide current count to reduce clutter
            holder.tvTarget.setText("COMPLETED"); // Explicit text
            holder.tvCurrentProgress.setTextColor(Color.WHITE);
            holder.tvTarget.setTextColor(Color.WHITE);
            holder.ivMore.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
    }

    private void showPopupMenu(View view, Habit habit) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(context, view);
        popup.getMenu().add("Edit");
        popup.getMenu().add("Delete");
        popup.setOnMenuItemClickListener(item -> {
            if (Objects.equals(item.getTitle(), "Edit")) {
                listener.onEditHabit(habit);
                return true;
            } else if (Objects.equals(item.getTitle(), "Delete")) {
                listener.onDeleteHabit(habit);
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    public void setData(List<Habit> list) {
        habits.clear();
        habits.addAll(list);
        notifyDataSetChanged();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView title, reason;
        ImageView icon, ivMore;
        View gradientView;
        View layoutProgressInfo;
        View actionContainer;
        TextView tvMark, tvCurrentProgress, tvTarget;
        ImageView btnMinus, btnPlus;
        View divider;
        ProgressBar pbGoal;

        HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.habitTitle);
            reason = itemView.findViewById(R.id.habitReason);
            icon = itemView.findViewById(R.id.habitTileIcon);
            ivMore = itemView.findViewById(R.id.ivMore);
            gradientView = itemView.findViewById(R.id.gradientView);
            layoutProgressInfo = itemView.findViewById(R.id.layoutProgressInfo);
            actionContainer = itemView.findViewById(R.id.actionContainer);
            tvMark = itemView.findViewById(R.id.tvMark);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            divider = itemView.findViewById(R.id.divider);
            tvCurrentProgress = itemView.findViewById(R.id.tvCurrentProgress);
            tvTarget = itemView.findViewById(R.id.tvTarget);
            pbGoal = itemView.findViewById(R.id.pbGoal);
        }
    }
}
