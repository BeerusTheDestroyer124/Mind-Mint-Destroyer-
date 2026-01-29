package com.gxdevs.mindmint.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_stats", indices = { @Index("date") })
public class DailyStatsEntity {
    @PrimaryKey
    @NonNull
    public String date; // yyyy-MM-dd format

    public int tasks_completed; // Number of tasks completed on this date
    public int habits_completed; // Number of habits completed on this date

    public long total_screen_time_seconds;
    public long total_scrolls;

    // Default constructor for Room
    public DailyStatsEntity() {
        date = "";
    }

    public DailyStatsEntity(@NonNull String date, int tasks_completed, int habits_completed, long total_screen_time_seconds, long total_scrolls) {
        this.date = date;
        this.tasks_completed = tasks_completed;
        this.habits_completed = habits_completed;
        this.total_screen_time_seconds = total_screen_time_seconds;
        this.total_scrolls = total_scrolls;
    }

    // Constructor for backward compatibility (optional but good for existing code)
    public DailyStatsEntity(@NonNull String date, int tasks_completed, int habits_completed) {
        this(date, tasks_completed, habits_completed, 0, 0);
    }
}
