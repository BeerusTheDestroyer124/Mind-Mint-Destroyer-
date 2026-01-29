package com.gxdevs.mindmint.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.gxdevs.mindmint.db.entities.HabitCompletionEntity;
import com.gxdevs.mindmint.db.models.DayOfWeekCount;
import com.gxdevs.mindmint.db.models.DayPartCount;

import java.util.List;

@Dao
public interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId ORDER BY completion_date_ms DESC")
    List<HabitCompletionEntity> getByHabitId(String habitId);

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND completion_date_ms >= :startDate AND completion_date_ms <= :endDate ORDER BY completion_date_ms DESC")
    List<HabitCompletionEntity> getByHabitIdAndDateRange(String habitId, long startDate, long endDate);

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND completion_date_ms = :dateMs LIMIT 1")
    HabitCompletionEntity getByHabitIdAndDate(String habitId, long dateMs);

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habit_id = :habitId AND is_completed = 1")
    int getCompletionCount(String habitId);

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habit_id = :habitId AND completion_date_ms >= :startDate AND completion_date_ms <= :endDate AND is_completed = 1")
    int getCompletionCountForDateRange(String habitId, long startDate, long endDate);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HabitCompletionEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HabitCompletionEntity> entities);

    @Delete
    void delete(HabitCompletionEntity entity);

    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId AND completion_date_ms = :dateMs")
    void deleteByHabitIdAndDate(String habitId, long dateMs);

    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId AND completion_date_ms >= :startDate AND completion_date_ms <= :endDate")
    void deleteByHabitIdAndDateRange(String habitId, long startDate, long endDate);

    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId")
    void deleteByHabitId(String habitId);

    @Query("DELETE FROM habit_completions")
    void deleteAll();

    // Date-based queries for statistics
    @Query("SELECT COUNT(DISTINCT habit_id) FROM habit_completions WHERE completion_date_ms >= :startDateMs AND completion_date_ms < :endDateMs AND is_completed = 1")
    int getUniqueHabitsCompletedInDateRange(long startDateMs, long endDateMs);

    @Query("SELECT COUNT(DISTINCT habit_id) FROM habit_completions WHERE completion_date_ms >= :startOfDayMs AND completion_date_ms <= :endOfDayMs AND is_completed = 1")
    int getUniqueHabitsCompletedForDate(long startOfDayMs, long endOfDayMs);

    @Query("SELECT * FROM habit_completions WHERE completion_date_ms >= :startDateMs AND completion_date_ms <= :endDateMs ORDER BY completion_date_ms ASC")
    List<HabitCompletionEntity> getCompletionsInDateRange(long startDateMs, long endDateMs);

    // ============ NEW QUERIES FOR ENHANCED HABITS ============

    /**
     * Get completions by day of week for heatmap.
     * Returns day_of_week as "0"-"6" (0 = Sunday)
     */
    @Query("SELECT CAST(strftime('%w', completion_date_ms / 1000, 'unixepoch', 'localtime') AS TEXT) as day_of_week, " +
            "COUNT(*) as count FROM habit_completions WHERE habit_id = :habitId AND is_completed = 1 GROUP BY day_of_week")
    List<DayOfWeekCount> getCompletionsByDayOfWeek(String habitId);

    /**
     * Get completions by day part for pattern analysis.
     */
    @Query("SELECT day_part, COUNT(*) as count FROM habit_completions WHERE habit_id = :habitId AND day_part IS NOT NULL AND is_completed = 1 GROUP BY day_part")
    List<DayPartCount> getCompletionsByDayPart(String habitId);

    /**
     * Get completions by day part for ALL habits (Global stats)
     */
    @Query("SELECT day_part, COUNT(*) as count FROM habit_completions WHERE day_part IS NOT NULL AND is_completed = 1 GROUP BY day_part")
    List<DayPartCount> getAllCompletionsByDayPart();

    @Query("SELECT day_part, COUNT(*) as count FROM habit_completions WHERE day_part IS NOT NULL AND is_completed = 1 AND completion_date_ms >= :startMs AND completion_date_ms < :endMs GROUP BY day_part")
    List<DayPartCount> getAllCompletionsByDayPartInRange(long startMs, long endMs);

    /**
     * Get completions for a specific week (by adjusted_date_ms range)
     */
    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND adjusted_date_ms >= :weekStartMs AND adjusted_date_ms < :weekEndMs ORDER BY completion_date_ms ASC")
    List<HabitCompletionEntity> getCompletionsForWeek(String habitId, long weekStartMs, long weekEndMs);

    /**
     * Get sum of counts for a specific adjusted date (for multi-tap habits)
     */
    @Query("SELECT COALESCE(SUM(count), 0) FROM habit_completions WHERE habit_id = :habitId AND adjusted_date_ms = :adjustedDateMs")
    int getCountForAdjustedDate(String habitId, long adjustedDateMs);

    /**
     * Get total volume (sum of counts) in a date range
     */
    @Query("SELECT COALESCE(SUM(count), 0) FROM habit_completions WHERE habit_id = :habitId AND completion_date_ms >= :startMs AND completion_date_ms < :endMs")
    int getVolumeInRange(String habitId, long startMs, long endMs);

    /**
     * Get total volume for ALL habits in a range (Global stats)
     * UDPATE: Changed to COUNT(*) to track "Habits Completed" instead of "Units"
     */
    @Query("SELECT COUNT(*) FROM habit_completions WHERE completion_date_ms >= :startMs AND completion_date_ms < :endMs AND is_completed = 1")
    int getAllCompletionsVolumeInRange(long startMs, long endMs);

    /**
     * Get total completions (days completed)
     */
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habit_id = :habitId AND is_completed = 1")
    int getTotalCompletionCount(String habitId);

    /**
     * Get all completion timestamps for interval calculation (ordered)
     */
    @Query("SELECT completion_date_ms FROM habit_completions WHERE habit_id = :habitId AND is_completed = 1 ORDER BY completion_date_ms ASC")
    List<Long> getCompletionTimestamps(String habitId);

    /**
     * Get distinct adjusted dates count this week (for tracking unique days
     * completed)
     */
    @Query("SELECT COUNT(DISTINCT adjusted_date_ms) FROM habit_completions WHERE habit_id = :habitId AND adjusted_date_ms >= :weekStartMs AND adjusted_date_ms < :weekEndMs AND is_completed = 1")
    int getUniqueDaysCompletedInWeek(String habitId, long weekStartMs, long weekEndMs);

    /**
     * Get latest completion for a habit
     */
    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId ORDER BY completion_date_ms DESC LIMIT 1")
    HabitCompletionEntity getLatestCompletion(String habitId);

    /**
     * Get completions by adjusted date
     */
    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND adjusted_date_ms = :adjustedDateMs ORDER BY completion_date_ms ASC")
    List<HabitCompletionEntity> getByAdjustedDate(String habitId, long adjustedDateMs);

    /**
     * Get daily completion counts for ALL habits (Global Heatmap)
     * Returns date_ms and count of unique habits completed that day
     */
    @Query("SELECT adjusted_date_ms as date_ms, COUNT(DISTINCT habit_id) as count FROM habit_completions WHERE is_completed = 1 AND adjusted_date_ms >= :startMs AND adjusted_date_ms <= :endMs GROUP BY adjusted_date_ms")
    List<com.gxdevs.mindmint.db.models.DateCount> getUniqueHabitsCountPerDay(long startMs, long endMs);

    /**
     * Get emotion counts for a habit within a date range.
     */
    @Query("SELECT emotion, COUNT(*) as count FROM habit_completions WHERE habit_id = :habitId AND emotion IS NOT NULL AND emotion != '' AND completion_date_ms >= :startDate AND completion_date_ms <= :endDate GROUP BY emotion")
    List<com.gxdevs.mindmint.db.models.EmotionCount> getEmotionCounts(String habitId, long startDate, long endDate);

    /**
     * Get emotion counts for ALL habits within a date range.
     */
    @Query("SELECT emotion, COUNT(*) as count FROM habit_completions WHERE emotion IS NOT NULL AND emotion != '' AND completion_date_ms >= :startDate AND completion_date_ms <= :endDate GROUP BY emotion")
    List<com.gxdevs.mindmint.db.models.EmotionCount> getGlobalEmotionCounts(long startDate, long endDate);

    @Query("SELECT * FROM habit_completions")
    List<HabitCompletionEntity> getAll();
}
