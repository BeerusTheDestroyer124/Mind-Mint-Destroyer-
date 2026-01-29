package com.gxdevs.mindmint.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_sessions")
public class FocusSessionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long start_time_ms;
    public long end_time_ms;
    public long duration_ms;
    public String date_str; // YYYY-MM-DD
    public String topic_name;
}
