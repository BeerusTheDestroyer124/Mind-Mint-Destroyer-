package com.gxdevs.mindmint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.gxdevs.mindmint.db.entities.AppUsageStatsEntity;

import java.util.List;

@Dao
public interface AppUsageStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppUsageStatsEntity> stats);

    @Query("SELECT * FROM app_usage_stats WHERE date = :date")
    List<AppUsageStatsEntity> getUsageForDate(String date);

    @Query("DELETE FROM app_usage_stats WHERE date = :date")
    void deleteUsageForDate(String date);

    @Query("SELECT SUM(duration_seconds) FROM app_usage_stats WHERE date = :date")
    long getTotalDurationForDate(String date);

    @Query("SELECT SUM(scroll_count) FROM app_usage_stats WHERE date = :date")
    long getTotalScrollsForDate(String date);
}
