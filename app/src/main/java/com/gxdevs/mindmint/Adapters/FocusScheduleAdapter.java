package com.gxdevs.mindmint.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.db.entities.FocusScheduleEntity;

import java.util.List;
import java.util.Locale;

public class FocusScheduleAdapter extends RecyclerView.Adapter<FocusScheduleAdapter.ViewHolder> {

    private final Context context;
    private final List<FocusScheduleEntity> schedules;
    private final OnScheduleActionListener listener;

    public interface OnScheduleActionListener {
        void onToggle(FocusScheduleEntity schedule, boolean isChecked);
        void onDelete(FocusScheduleEntity schedule);
        void onClick(FocusScheduleEntity schedule);
    }

    public FocusScheduleAdapter(Context context, List<FocusScheduleEntity> schedules, OnScheduleActionListener listener) {
        this.context = context;
        this.schedules = schedules;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_focus_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FocusScheduleEntity schedule = schedules.get(position);

        int displayHour = schedule.startHour % 12;
        if (displayHour == 0) displayHour = 12;
        String amPm = schedule.startHour >= 12 ? "PM" : "AM";
        String timeText = String.format(Locale.US, "%02d:%02d %s", displayHour, schedule.startMinute, amPm);
        holder.timeText.setText(timeText);
        holder.daysText.setText(schedule.daysOfWeek != null ? schedule.daysOfWeek : "NONE");
        
        String configText = schedule.durationMinutes + " min";
        if (schedule.label != null && !schedule.label.isEmpty()) {
            configText += " • " + schedule.label;
        }
        if (schedule.isLockedIn == 1) {
            configText += " • Locked";
        }
        holder.configText.setText(configText);

        holder.switchBtn.setOnCheckedChangeListener(null);
        holder.switchBtn.setChecked(schedule.isEnabled == 1);

        holder.switchBtn.setOnCheckedChangeListener((btn, isChecked) -> listener.onToggle(schedule, isChecked));

        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(schedule));
        holder.itemView.setOnClickListener(v -> listener.onClick(schedule));
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, daysText, configText;
        MaterialSwitch switchBtn;
        ImageView deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.scheduleTimeText);
            daysText = itemView.findViewById(R.id.scheduleDaysText);
            configText = itemView.findViewById(R.id.scheduleConfigText);
            switchBtn = itemView.findViewById(R.id.scheduleSwitch);
            deleteBtn = itemView.findViewById(R.id.scheduleDeleteBtn);
        }
    }
}
