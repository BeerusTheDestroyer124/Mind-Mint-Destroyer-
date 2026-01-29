package com.gxdevs.mindmint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gxdevs.mindmint.db.entities.HabitEntity;

import java.util.List;

@Dao
public interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY created_at_ms ASC")
    List<HabitEntity> getAll();

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    HabitEntity getById(String habitId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HabitEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HabitEntity> entities);

    @Update
    void update(HabitEntity entity);

    @Query("DELETE FROM habits WHERE id = :habitId")
    void deleteById(String habitId);

    @Query("SELECT COUNT(*) FROM habits WHERE frequency_goal = 'DAILY' AND created_at_ms <= :dateMs")
    int getCountActiveHabitsBeforeDate(long dateMs);

    @Query("DELETE FROM habits")
    void deleteAll();

    // Streak queries
    @Query("SELECT MAX(current_streak_days) FROM habits")
    int getMaxCurrentStreak();

    @Query("SELECT * FROM habits ORDER BY current_streak_days DESC")
    List<HabitEntity> getAllSortedByStreak();

    // ============ NEW QUERIES FOR ENHANCED HABITS ============

    /**
     * Get habits filtered by frequency goal type
     */
    @Query("SELECT * FROM habits WHERE frequency_goal = :frequencyGoal ORDER BY created_at_ms ASC")
    List<HabitEntity> getByFrequencyGoal(String frequencyGoal);

    /**
     * Get habits filtered by priority
     */
    @Query("SELECT * FROM habits WHERE priority = :priority ORDER BY created_at_ms ASC")
    List<HabitEntity> getByPriority(String priority);

    /**
     * Get all habits ordered by priority (HIGH first) then by creation date
     */
    @Query("SELECT * FROM habits ORDER BY " +
            "CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END, " +
            "created_at_ms ASC")
    List<HabitEntity> getAllByPriority();

    /**
     * Get daily habits (for reminders/streak checking)
     */
    @Query("SELECT * FROM habits WHERE frequency_goal = 'DAILY' ORDER BY created_at_ms ASC")
    List<HabitEntity> getDailyHabits();

    /**
     * Get times-per-week habits (for weekly tracking)
     */
    @Query("SELECT * FROM habits WHERE frequency_goal = 'TIMES_PER_WEEK' ORDER BY created_at_ms ASC")
    List<HabitEntity> getTimesPerWeekHabits();

    /**
     * Get unlimited/counter habits
     */
    @Query("SELECT * FROM habits WHERE frequency_goal = 'UNLIMITED' ORDER BY created_at_ms ASC")
    List<HabitEntity> getUnlimitedHabits();

    /**
     * Count habits by frequency goal
     */
    @Query("SELECT COUNT(*) FROM habits WHERE frequency_goal = :frequencyGoal")
    int countByFrequencyGoal(String frequencyGoal);

    /**
     * Reset progress for daily habits that haven't been updated today
     */
    @Query("UPDATE habits SET current_progress = 0 WHERE frequency_goal = 'DAILY' AND last_progress_date_ms < :todayStartMs")
    void resetDailyHabitProgress(long todayStartMs);
}
