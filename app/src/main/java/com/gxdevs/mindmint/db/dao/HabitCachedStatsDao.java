package com.gxdevs.mindmint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.gxdevs.mindmint.db.entities.HabitCachedStatsEntity;

import java.util.List;

/**
 * DAO for habit cached statistics.
 */
@Dao
public interface HabitCachedStatsDao {

    @Query("SELECT * FROM habit_cached_stats WHERE habit_id = :habitId")
    HabitCachedStatsEntity getByHabitId(String habitId);

    @Query("SELECT * FROM habit_cached_stats")
    List<HabitCachedStatsEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(HabitCachedStatsEntity entity);

    @Query("DELETE FROM habit_cached_stats WHERE habit_id = :habitId")
    void deleteByHabitId(String habitId);

    @Query("DELETE FROM habit_cached_stats")
    void deleteAll();

    /**
     * Get habits that need stats refresh (older than specified time)
     */
    @Query("SELECT * FROM habit_cached_stats WHERE last_updated_ms < :thresholdMs")
    List<HabitCachedStatsEntity> getStaleStats(long thresholdMs);
}
