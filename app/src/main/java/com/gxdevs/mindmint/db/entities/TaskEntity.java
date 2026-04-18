package com.gxdevs.mindmint.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String short_description;
    public int image_resource;
    public int is_completed;
    public int priority_value;
    public long created_date_ms;
    public long completed_date_ms;
    public long scheduled_date_ms;
    public String recurring_type;
    public int is_recurring;
    public int has_reminder;
    public String repeat_options_json;
    public int is_habit;
    public String habit_id;
    /**
     * Name of the drawable resource for the task icon (e.g., "flame", "droplets").
     *
     * Storing the name instead of the raw int ID makes the value stable across
     * builds and drawable reordering. The app will resolve this name back to a
     * safe drawable at runtime with a fallback to a default icon if missing.
     */
    public String icon_name;

    // Exact string representation of completion time as requested
    public String completed_date_str; // e.g., "2023-10-27"
    public String completed_time_str; // e.g., "14:30:00"

    // --- Focus Mode fields ---
    public int focus_mode_enabled;       // 0 = false, 1 = true
    public int focus_duration_minutes;   // 0 = open-ended, 1-180 = timed
    public long focus_time_spent_ms;     // cumulative ms spent in focus on this task
    public String focus_status;          // "IDLE", "IN_PROGRESS", "PAUSED"
}
