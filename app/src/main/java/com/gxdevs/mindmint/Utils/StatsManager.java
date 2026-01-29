package com.gxdevs.mindmint.Utils;

import android.content.Context;

import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.DailyStatsDao;
import com.gxdevs.mindmint.db.dao.HabitCompletionDao;
import com.gxdevs.mindmint.db.dao.TaskDao;
import com.gxdevs.mindmint.db.entities.DailyStatsEntity;
import com.gxdevs.mindmint.db.entities.HabitCompletionEntity;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

public class StatsManager {
    private final DailyStatsDao dailyStatsDao;
    private final TaskDao taskDao;
    private final HabitCompletionDao habitCompletionDao;
    private final Context context;

    public StatsManager(Context context) {
        this.context = context;
        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(context);
        this.dailyStatsDao = db.dailyStatsDao();
        this.taskDao = db.taskDao();
        this.habitCompletionDao = db.habitCompletionDao();
    }

    public void updateTodayStats() {
        String today = DateUtils.getTodayDateString();
        long startOfDay = DateUtils.getStartOfTodayMs();
        long endOfDay = DateUtils.getEndOfTodayMs();

        int tasksCompleted = taskDao.getCompletedCountForDate(startOfDay, endOfDay);
        int habitsCompleted = habitCompletionDao.getUniqueHabitsCompletedForDate(startOfDay, endOfDay);

        DailyStatsEntity stats = dailyStatsDao.getByDate(today);
        if (stats == null) {
            stats = new DailyStatsEntity(today, tasksCompleted, habitsCompleted);
        } else {
            stats.tasks_completed = tasksCompleted;
            stats.habits_completed = habitsCompleted;
        }
        dailyStatsDao.insertOrReplace(stats);
    }

    public int calculateHabitStreak(String habitId) {
        List<HabitCompletionEntity> completions = habitCompletionDao.getByHabitId(habitId);
        if (completions.isEmpty()) return 0;

        completions.sort((a, b) -> Long.compare(b.completion_date_ms, a.completion_date_ms));

        // Calculate consecutive days from today backwards
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int streak = 0;
        int completionIndex = 0;

        // Start from today and go backwards
        for (int daysBack = 0; daysBack < 365; daysBack++) { // Check up to 1 year
            long dayStart = cal.getTimeInMillis();
            long dayEnd = dayStart + (24 * 60 * 60 * 1000) - 1;

            boolean found = false;
            while (completionIndex < completions.size()) {
                HabitCompletionEntity completion = completions.get(completionIndex);
                if (completion.completion_date_ms >= dayStart && completion.completion_date_ms <= dayEnd) {
                    found = true;
                    streak++;
                    completionIndex++;
                    break;
                } else if (completion.completion_date_ms < dayStart) {
                    break;
                }
                completionIndex++;
            }

            if (!found) {
                if (daysBack > 1) {
                    break;
                }
            }

            // Move to previous day
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    /**
     * Get the highest streak ever achieved for a habit
     */
    public int getHighestStreakForHabit(String habitId) {
        List<HabitCompletionEntity> completions = habitCompletionDao.getByHabitId(habitId);
        if (completions.isEmpty()) return 0;

        // Sort by date ascending
        completions.sort(Comparator.comparingLong(a -> a.completion_date_ms));

        int maxStreak = 0;
        int currentStreak = 0;
        long lastDate = 0;

        for (HabitCompletionEntity completion : completions) {
            long completionDate = getStartOfDay(completion.completion_date_ms);

            if (lastDate == 0) {
                // First completion
                currentStreak = 1;
            } else {
                long daysDiff = (completionDate - lastDate) / (24 * 60 * 60 * 1000);
                if (daysDiff == 1) {
                    // Consecutive day
                    currentStreak++;
                } else {
                    // Streak broken, reset
                    maxStreak = Math.max(maxStreak, currentStreak);
                    currentStreak = 1;
                }
            }
            lastDate = completionDate;
        }

        return Math.max(maxStreak, currentStreak);
    }

    /**
     * Get start of day for a timestamp
     */
    private long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Check and update streaks for all habits at midnight
     * This should be called by the midnight reset receiver
     */
    public void checkAndUpdateStreaksAtMidnight() {
        HabitManager habitManager = new HabitManager(context);
        List<Habit> habits = habitManager.loadHabits();

        String yesterday = DateUtils.getYesterdayDateString();
        long yesterdayStart = DateUtils.getStartOfDayMs(yesterday);
        long yesterdayEnd = DateUtils.getEndOfDayMs(yesterday);

        for (Habit habit : habits) {
            // Check if habit was completed yesterday
            List<HabitCompletionEntity> yesterdayCompletions = habitCompletionDao.getCompletionsInDateRange(
                    yesterdayStart, yesterdayEnd
            );

            boolean completedYesterday = false;
            for (HabitCompletionEntity completion : yesterdayCompletions) {
                if (completion.habit_id.equals(habit.getId())) {
                    completedYesterday = true;
                    break;
                }
            }

            if (!completedYesterday && habit.getCurrentStreakDays() > 0) {
                // Streak broken, reset to 0
                habit.setCurrentStreakDays(0);
                habitManager.updateHabit(habit);
            } else if (completedYesterday) {
                // Update streak based on actual completion dates
                int newStreak = calculateHabitStreak(habit.getId());
                if (newStreak != habit.getCurrentStreakDays()) {
                    habit.setCurrentStreakDays(newStreak);
                    habitManager.updateHabit(habit);
                }
            }
        }

        // Update today's stats
        updateTodayStats();
    }
}

