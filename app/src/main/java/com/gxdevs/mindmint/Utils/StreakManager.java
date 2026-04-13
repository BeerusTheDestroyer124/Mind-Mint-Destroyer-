package com.gxdevs.mindmint.Utils;

import android.content.Context;
import android.util.Pair;

import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.HabitCompletionDao;
import com.gxdevs.mindmint.db.dao.HabitDao;
import com.gxdevs.mindmint.db.entities.HabitCompletionEntity;
import com.gxdevs.mindmint.db.entities.HabitEntity;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class StreakManager {

    private final StreakPrefs streakPrefs;
    private final HabitDao habitDao;
    private final HabitCompletionDao habitCompletionDao;

    public StreakManager(Context context) {
        this.streakPrefs = new StreakPrefs(context);
        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(context);
        this.habitDao = db.habitDao();
        this.habitCompletionDao = db.habitCompletionDao();
    }

    public void updateStreakOnHabitCompletion() {
        recalculateOverallStreak();
    }

    public void checkAndResetStreakIfNeeded(List<Habit> allHabits) {
        recalculateOverallStreak();
    }

    public void recalculateOverallStreak() {
        List<HabitEntity> allHabits = habitDao.getAll();
        if (allHabits.isEmpty()) {
            streakPrefs.setStreak(0);
            return;
        }

        int streak = 0;
        int expectedCount = allHabits.size();

        // Normalize calendar to start of today (midnight)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Check today first — if today is not fully completed, start checking from yesterday
        long todayStart = cal.getTimeInMillis();
        long todayEnd = todayStart + (24L * 60 * 60 * 1000) - 1;
        boolean todayComplete = habitCompletionDao.getUniqueHabitsCompletedForDate(todayStart, todayEnd) >= expectedCount;

        if (!todayComplete) {
            // Start counting from yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Walk backwards day by day — stop the moment a day is broken (consecutive streak)
        for (int daysBack = 0; daysBack < 730; daysBack++) {
            long dayStart = cal.getTimeInMillis();
            long dayEnd = dayStart + (24L * 60 * 60 * 1000) - 1;

            int actualCount = habitCompletionDao.getUniqueHabitsCompletedForDate(dayStart, dayEnd);
            boolean allCompleted = actualCount >= expectedCount;

            if (allCompleted) {
                streak++;
            } else {
                // Streak is broken — stop counting
                break;
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        streakPrefs.setStreak(streak);
    }

    public int getGlobalStreak() {
        recalculateOverallStreak();
        return streakPrefs.getStreak();
    }

    /**
     * Calculates the progress for Today (Active Habits).
     *
     * @param todayHabits List of habits to check (should be loaded for today).
     * @return Pair of (CompletedCount, TotalCount)
     */
    public Pair<Integer, Integer> getTodayProgress(List<Habit> todayHabits) {
        if (todayHabits == null || todayHabits.isEmpty()) {
            return new Pair<>(0, 0);
        }

        int total = todayHabits.size();
        int completed = 0;
        for (Habit h : todayHabits) {
            if (h.isDoneToday()) {
                completed++;
            }
        }
        return new Pair<>(completed, total);
    }

    public void recalculateHabitStreaks(String habitId) {
        List<HabitCompletionEntity> completions = habitCompletionDao.getByHabitId(habitId);
        completions.sort(Comparator.comparingLong(a -> a.completion_date_ms));

        int maxStreak = 0;
        int currentRun = 0;
        long lastDateMs = 0;

        // Calculate Max Streak
        for (HabitCompletionEntity c : completions) {
            long dateMs = DateUtils.getStartOfDayMs(new Date(c.completion_date_ms));

            if (lastDateMs == 0) {
                currentRun = 1;
            } else {
                long diff = dateMs - lastDateMs;
                long daysDiff = diff / (24 * 60 * 60 * 1000); // approx

                if (daysDiff == 0) {
                    // Duplicate day, ignore
                    continue;
                } else if (daysDiff == 1) {
                    currentRun++;
                } else {
                    currentRun = 1;
                }
            }
            if (currentRun > maxStreak) {
                maxStreak = currentRun;
            }
            lastDateMs = dateMs;
        }

        int currentStreak = 0;
        // Sort descending for backwards check
        completions.sort((a, b) -> Long.compare(b.completion_date_ms, a.completion_date_ms));

        Calendar cal = Calendar.getInstance();

        boolean isTodayCompleted = false;

        // Check if completed today
        for (HabitCompletionEntity c : completions) {
            if (DateUtils.isSameDay(new Date(c.completion_date_ms), new Date())) {
                isTodayCompleted = true;
                break;
            }
        }

        if (isTodayCompleted) {
            currentStreak = 1;
            // Next check is yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1);
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Safe loop backwards
        for (int i = 0; i < 365 * 2; i++) { // Check 2 years back max
            long checkDayStart = DateUtils.getStartOfDayMs(cal.getTime());
            long checkDayEnd = DateUtils.getEndOfDayMs(cal.getTime());

            boolean found = false;
            for (HabitCompletionEntity c : completions) {
                if (c.completion_date_ms >= checkDayStart && c.completion_date_ms <= checkDayEnd) {
                    found = true;
                    break;
                }
            }

            if (found) {
                if (!isTodayCompleted && currentStreak == 0) {
                    // This is the first day of streak (yesterday)
                    currentStreak = 1;
                } else {
                    currentStreak++;
                }
            } else {
                // Streak broken
                break;
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // 4. Update Habit Entity
        HabitEntity habitEntity = habitDao.getById(habitId);
        if (habitEntity != null) {
            habitEntity.current_streak_days = currentStreak;
            habitEntity.max_streak_days = maxStreak;

            if (!completions.isEmpty()) {
                habitEntity.last_completed_date_ms = completions.get(0).completion_date_ms;
            } else {
                habitEntity.last_completed_date_ms = 0;
            }

            habitDao.update(habitEntity);
        }
    }
}
