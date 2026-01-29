package com.gxdevs.mindmint.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_state")
public class FocusStateEntity {
    @PrimaryKey
    public int id; // always 1
    public boolean active;
    public long end_elapsed; // elapsedRealtime() when timer should end
    public long duration; // total duration for this session

    // New fields for Pomodoro & Resume
    public long start_time; // Original start time (wall clock)
    public long current_segment_start; // Start of current Focus or Break segment
    public long accumulated_focus; // Focus time banked before current segment
    public boolean is_break; // True if currently in break

    // Config
    public boolean is_pomodoro;
    public long pomodoro_focus_interval;
    public long pomodoro_break_interval;
    public String topic_name;

    // Pause & Animation State
    public boolean is_paused; // True when in break or waiting for user
    public float crystal_reveal_fraction; // Persist animation position (0.0 - 1.0)
    public long break_start_time; // When break started
    public long idle_timeout_start; // When waiting_user state started (for 20-min timeout)
}
