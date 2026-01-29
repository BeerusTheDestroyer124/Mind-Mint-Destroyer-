package com.gxdevs.mindmint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gxdevs.mindmint.db.entities.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY created_date_ms ASC")
    List<TaskEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TaskEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TaskEntity> entities);

    @Update
    void update(TaskEntity entity);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteById(String taskId);

    @Query("DELETE FROM tasks")
    void deleteAll();

    // Date-based queries for statistics
    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1 AND completed_date_ms >= :startDateMs AND completed_date_ms < :endDateMs")
    int getCompletedCountInDateRange(long startDateMs, long endDateMs);

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1 AND completed_date_ms >= :startOfDayMs AND completed_date_ms < :endOfDayMs")
    int getCompletedCountForDate(long startOfDayMs, long endOfDayMs);

    @Query("SELECT strftime('%H', completed_time_str) as hour, COUNT(*) as count FROM tasks WHERE is_completed = 1 AND completed_time_str IS NOT NULL GROUP BY hour")
    List<com.gxdevs.mindmint.db.models.HourCount> getCompletedTasksByHour();

    @Query("SELECT CAST(strftime('%w', completed_date_str) AS TEXT) as day_of_week, COUNT(*) as count FROM tasks WHERE is_completed = 1 AND completed_date_str IS NOT NULL GROUP BY day_of_week")
    List<com.gxdevs.mindmint.db.models.DayOfWeekCount> getCompletedTasksByDayOfWeek();

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0")
    int getPendingCount();
}
