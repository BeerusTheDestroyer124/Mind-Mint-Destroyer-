package com.gxdevs.mindmint.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_usage_stats")
public class AppUsageStatsEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String date; // yyyy-MM-dd
    public String package_name;
    public long duration_seconds;
    public long scroll_count;
    public long last_updated_ms;

    public AppUsageStatsEntity(@NonNull String date, String package_name, long duration_seconds, long scroll_count) {
        this.date = date;
        this.package_name = package_name;
        this.duration_seconds = duration_seconds;
        this.scroll_count = scroll_count;
        this.last_updated_ms = System.currentTimeMillis();
    }
}
