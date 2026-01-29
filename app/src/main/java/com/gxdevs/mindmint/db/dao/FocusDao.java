package com.gxdevs.mindmint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gxdevs.mindmint.db.entities.FocusDailyStatEntity;
import com.gxdevs.mindmint.db.entities.FocusSessionEntity;
import com.gxdevs.mindmint.db.entities.FocusStateEntity;

import java.util.List;

@Dao
public interface FocusDao {
    // State
    @Query("SELECT * FROM focus_state WHERE id = 1")
    FocusStateEntity getState();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceState(FocusStateEntity state);

    // Daily stats
    @Query("SELECT seconds FROM focus_daily_stats WHERE date = :date LIMIT 1")
    Long getSecondsForDate(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceDaily(FocusDailyStatEntity entity);

    @Update
    void updateDaily(FocusDailyStatEntity entity);

    @Query("SELECT COALESCE(SUM(seconds), 0) FROM focus_daily_stats")
    long getTotalSeconds();

    // Date-based queries for statistics
    @Query("SELECT * FROM focus_daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    List<FocusDailyStatEntity> getByDateRange(String startDate, String endDate);

    @Query("SELECT seconds FROM focus_daily_stats WHERE date = :date LIMIT 1")
    Long getSecondsForDateString(String date);

    // Sessions
    @Insert
    void insertSession(FocusSessionEntity session);

    @Query("SELECT * FROM focus_sessions WHERE start_time_ms >= :startMs AND start_time_ms <= :endMs ORDER BY start_time_ms ASC")
    List<FocusSessionEntity> getSessionsInDateRange(long startMs, long endMs);

    @Query("SELECT * FROM focus_daily_stats")
    List<FocusDailyStatEntity> getAllDailyStats();

    @Query("SELECT * FROM focus_sessions")
    List<FocusSessionEntity> getAllSessions();

    @Query("DELETE FROM focus_sessions")
    void deleteAllSessions();

    @Query("DELETE FROM focus_daily_stats")
    void deleteAllDailyStats();

    @Query("DELETE FROM focus_state")
    void deleteAllState();
}
