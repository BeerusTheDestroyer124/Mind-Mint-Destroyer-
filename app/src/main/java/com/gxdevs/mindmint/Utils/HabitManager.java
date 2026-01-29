package com.gxdevs.mindmint.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.db.Converters;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.HabitCompletionDao;
import com.gxdevs.mindmint.db.dao.HabitDao;
import com.gxdevs.mindmint.db.entities.HabitCompletionEntity;
import com.gxdevs.mindmint.db.entities.HabitEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HabitManager {
    private static final String PREFS_NAME = "HabitPrefs";
    private static boolean oldPrefsCleared = false;

    private final HabitDao habitDao;
    private final HabitCompletionDao habitCompletionDao;
    private final StreakManager streakManager;
    private final Context context;

    public HabitManager(Context context) {
        this.context = context;
        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(context);
        this.habitDao = db.habitDao();
        this.habitCompletionDao = db.habitCompletionDao();
        this.streakManager = new StreakManager(context);

        if (!oldPrefsCleared) {
            clearOldSharedPreferences(context);
            oldPrefsCleared = true;
        }
    }

    private static void clearOldSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public void saveHabits(List<Habit> habits) {
        habitDao.deleteAll();
        List<HabitEntity> entities = new ArrayList<>();
        for (Habit h : habits)
            entities.add(toHabitEntity(h));
        habitDao.insertAll(entities);
    }

    public List<Habit> loadHabits() {
        List<HabitEntity> entities = habitDao.getAll();
        List<Habit> out = new ArrayList<>();
        for (HabitEntity e : entities)
            out.add(fromHabitEntity(e));
        return out;
    }

    public List<Habit> getHabitsSortedByStreak() {
        List<Habit> allHabits = loadHabits();
        allHabits.sort((h1, h2) -> Integer.compare(h2.getCurrentStreakDays(), h1.getCurrentStreakDays()));
        return allHabits;
    }

    public void addHabit(Habit habit) {
        habitDao.insert(toHabitEntity(habit));
    }

    public void updateHabit(Habit habit) {
        habitDao.update(toHabitEntity(habit));
    }

    public void deleteHabit(String habitId) {
        habitDao.deleteById(habitId);
        habitCompletionDao.deleteByHabitId(habitId);
        streakManager.recalculateOverallStreak();
    }

    public Habit getHabitById(String habitId) {
        HabitEntity e = habitDao.getById(habitId);
        return e != null ? fromHabitEntity(e) : null;
    }

    private HabitEntity toHabitEntity(Habit h) {
        HabitEntity e = new HabitEntity();
        e.id = h.getId();
        e.name = h.getName();
        e.reason = h.getReason();
        e.difficulty = h.getDifficulty() != null ? h.getDifficulty().name() : "MEDIUM";
        e.duration_type = h.getDurationType() != null ? h.getDurationType().name() : "INDEFINITE";
        e.duration_days = h.getDurationDays();

        // Store icon as a stable drawable name instead of raw int ID
        int iconRes = h.getIcon();
        if (iconRes == 0) {
            iconRes = R.drawable.flame;
        }
        try {
            e.icon_name = context.getResources().getResourceEntryName(iconRes);
        } catch (Exception ignored) {
            e.icon_name = "flame";
        }

        e.icon_tint = h.getIconTint();
        e.icon_background_tint = h.getIconBackgroundTint();
        e.created_at_ms = toMs(h.getCreatedAt());
        e.last_completed_date_ms = toMs(h.getLastCompletedDate());
        e.current_streak_days = h.getCurrentStreakDays();
        e.max_streak_days = 0; // Not used anymore, kept for database compatibility
        e.milestone_index = h.getMilestoneIndex();
        e.is_ask_emotion = h.isAskEmotion();
        e.is_goal_tracking = h.isGoalTracking();
        e.one_tap_value = h.getOneTapValue();
        e.current_progress = h.getCurrentProgress();
        e.last_progress_date_ms = h.getLastProgressDateMs();
        e.target_count = h.getTargetCount();
        e.target_unit = h.getTargetUnit();
        return e;
    }

    @SuppressLint("DiscouragedApi")
    private Habit fromHabitEntity(HabitEntity e) {
        Habit h = new Habit();
        h.setId(e.id);
        h.setName(e.name);
        h.setReason(e.reason);
        try {
            h.setDifficulty(Habit.Difficulty.valueOf(e.difficulty));
        } catch (Exception ex) {
            h.setDifficulty(Habit.Difficulty.MEDIUM);
        }
        try {
            h.setDurationType(Habit.DurationType.valueOf(e.duration_type));
        } catch (Exception ex) {
            h.setDurationType(Habit.DurationType.INDEFINITE);
        }
        h.setDurationDays(e.duration_days);

        int iconRes = 0;
        if (e.icon_name != null && !e.icon_name.isEmpty()) {
            iconRes = context.getResources().getIdentifier(e.icon_name, "drawable", context.getPackageName());
        }
        if (iconRes == 0) {
            iconRes = R.drawable.flame;
        }
        h.setIcon(iconRes);
        h.setIconTint(e.icon_tint);
        h.setIconBackgroundTint(e.icon_background_tint);
        h.setCreatedAt(fromMs(e.created_at_ms));
        h.setLastCompletedDate(e.last_completed_date_ms > 0 ? new Date(e.last_completed_date_ms) : null);
        h.setCurrentStreakDays(e.current_streak_days);
        h.setMaxStreakDays(0);
        h.setMilestoneIndex(e.milestone_index);
        h.setAskEmotion(e.is_ask_emotion);
        h.setGoalTracking(e.is_goal_tracking);
        h.setOneTapValue(e.one_tap_value);
        h.setCurrentProgress(e.current_progress);
        h.setLastProgressDateMs(e.last_progress_date_ms);
        h.setTargetCount(e.target_count);
        h.setTargetUnit(e.target_unit);
        h.resetProgressIfNeeded();

        return h;
    }

    private long toMs(Date d) {
        return d != null ? d.getTime() : 0L;
    }

    private Date fromMs(long ms) {
        return ms > 0 ? new Date(ms) : null;
    }

    public boolean isCompletedToday(Habit habit) {
        if (habit.getLastCompletedDate() == null) return false;

        Calendar last = Calendar.getInstance();
        last.setTime(habit.getLastCompletedDate());

        Calendar now = Calendar.getInstance();
        return last.get(Calendar.YEAR) == now.get(Calendar.YEAR) && last.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
    }

    private boolean checkDbCompletion(String habitId, long dateMs) {
        long start = DateUtils.getStartOfDayMs(new Date(dateMs));
        long end = DateUtils.getEndOfDayMs(new Date(dateMs));
        return habitCompletionDao.getVolumeInRange(habitId, start, end) > 0;
    }

    public void markHabit(Habit habit) {
        markHabit(habit, null);
    }

    public void markHabit(Habit habit, String emotion) {
        long now = System.currentTimeMillis();

        if (isCompletedToday(habit)) {
            // Logic: If already done, maybe update data?
            if (emotion != null && !emotion.isEmpty()) {
                long start = DateUtils.getStartOfDayMs(DateUtils.getTodayDateString());
                long end = DateUtils.getEndOfDayMs(DateUtils.getTodayDateString());
                List<HabitCompletionEntity> list = habitCompletionDao.getByHabitIdAndDateRange(habit.getId(), start,
                        end);
                if (!list.isEmpty()) {
                    HabitCompletionEntity e = list.get(0);
                    e.emotion = emotion;
                    habitCompletionDao.insert(e); // Replace
                }
            }
            return;
        }

        Date today = new Date();
        habit.setLastCompletedDate(today);

        // Continue streak logic (Model Update)
        String yesterday = DateUtils.getYesterdayDateString();
        long yesterdayStart = DateUtils.getStartOfDayMs(yesterday);
        long yesterdayEnd = DateUtils.getEndOfDayMs(yesterday);
        List<HabitCompletionEntity> yesterdayCompletions = habitCompletionDao.getByHabitIdAndDateRange(
                habit.getId(), yesterdayStart, yesterdayEnd);

        if (!yesterdayCompletions.isEmpty()) {
            habit.setCurrentStreakDays(habit.getCurrentStreakDays() + 1);
        } else {
            habit.setCurrentStreakDays(1);
        }

        // DB Insert (Single Source of Truth)
        // Ensure no dupe for today
        if (!checkDbCompletion(habit.getId(), now)) {
            HabitCompletionEntity entity = new HabitCompletionEntity();
            entity.habit_id = habit.getId();
            entity.completion_date_ms = now;
            entity.day_part = Converters.calculateDayPart(now);
            entity.adjusted_date_ms = Converters.calculateAdjustedDate(now, 0);
            entity.count = 1;
            entity.is_completed = true;
            entity.emotion = emotion;
            habitCompletionDao.insert(entity);
        }

        // Update Progress Model
        int newProgress = getNewProgress(habit);

        habit.setCurrentProgress(newProgress);
        habit.setLastProgressDateMs(now);
        updateHabit(habit);

        // Recalculate
        streakManager.recalculateHabitStreaks(habit.getId());
        streakManager.recalculateOverallStreak();

        // Stats
        StatsManager statsManager = new StatsManager(context);
        statsManager.updateTodayStats();
    }

    private static int getNewProgress(Habit habit) {
        return habit.getCurrentProgress() + 1;
    }

    public void unmarkHabit(Habit habit) {
        if (!isCompletedToday(habit))
            return;

        Date today = new Date();

        // Model Reset
        habit.setLastCompletedDate(null);
        if (habit.getCurrentStreakDays() > 0)
            habit.setCurrentStreakDays(habit.getCurrentStreakDays() - 1);

        habit.setCurrentProgress(0); // Reset progress on unmark
        updateHabit(habit);

        // DB Removal (Safe Range)
        long start = DateUtils.getStartOfDayMs(today);
        long end = DateUtils.getEndOfDayMs(today);
        habitCompletionDao.deleteByHabitIdAndDateRange(habit.getId(), start, end);

        streakManager.recalculateHabitStreaks(habit.getId());
        streakManager.recalculateOverallStreak();

        StatsManager statsManager = new StatsManager(context);
        statsManager.updateTodayStats();
    }

    // UPDATED: Robust Progress Update
    public boolean updateHabitProgress(Habit habit, int progress) {
        if (progress < 0)
            progress = 0;

        // Cap progress at target count - cannot exceed target
        int target = habit.getTargetCount();
        if (target > 0 && progress > target) {
            progress = target;
        }

        // If already completed today and trying to add more, ignore
        if (isCompletedToday(habit) && progress >= target) {
            return false; // Already done, no change
        }

        boolean reachedTarget = progress >= habit.getTargetCount();

        if (reachedTarget) {
            // Ensure it is marked as COMPLETED
            if (!isCompletedToday(habit)) {
                // First time reaching target today
                habit.setCurrentProgress(progress);
                habit.setLastProgressDateMs(System.currentTimeMillis());

                markHabit(habit);
                // markHabit sets progress = current + 1, force it back
                habit.setCurrentProgress(progress);
                updateHabit(habit);

                // CRITICAL FIX: Update the DB count to match actual progress (e.g. 5) instead
                // of 1
                ensureTodayProgressRecord(habit, progress, true);

                return true;
            } else {
                // Already completed, just update progress number
                habit.setCurrentProgress(progress);
                habit.setLastProgressDateMs(System.currentTimeMillis());
                updateHabit(habit);

                // Update DB record count
                ensureTodayProgressRecord(habit, progress, true);
                return false;
            }
        } else {
            // Not reached target
            if (isCompletedToday(habit)) {
                // Was completed, now broken?
                unmarkHabit(habit);
                // unmark resets progress to 0. Restore logic:
                habit.setCurrentProgress(progress);
                updateHabit(habit);
            } else {
                // Just update progress
                habit.setCurrentProgress(progress);
                habit.setLastProgressDateMs(System.currentTimeMillis());
                updateHabit(habit);
            }

            // SAVE PARTIAL PROGRESS TO DB
            if (progress > 0) {
                ensureTodayProgressRecord(habit, progress, false);
            } else {
                // If progress is 0, ensure no record exists for today (clean up)
                long start = DateUtils.getStartOfDayMs(DateUtils.getTodayDateString());
                long end = DateUtils.getEndOfDayMs(DateUtils.getTodayDateString());
                habitCompletionDao.deleteByHabitIdAndDateRange(habit.getId(), start, end);
            }

            return false;
        }
    }

    private void ensureTodayProgressRecord(Habit habit, int count, boolean isCompleted) {
        long now = System.currentTimeMillis();
        long start = DateUtils.getStartOfDayMs(DateUtils.getTodayDateString());
        long end = DateUtils.getEndOfDayMs(DateUtils.getTodayDateString());

        List<HabitCompletionEntity> list = habitCompletionDao.getByHabitIdAndDateRange(habit.getId(), start, end);
        HabitCompletionEntity entity;

        if (!list.isEmpty()) {
            entity = list.get(0);
            // Don't change emotion or other fields if they exist
        } else {
            entity = new HabitCompletionEntity();
            entity.habit_id = habit.getId();
            entity.completion_date_ms = now;
            entity.day_part = Converters.calculateDayPart(now);
            entity.adjusted_date_ms = Converters.calculateAdjustedDate(now, 0);
        }
        entity.count = count;
        entity.is_completed = isCompleted;
        habitCompletionDao.insert(entity);
    }

    public void toggleHistoryCompletion(String habitId, long dateMs, boolean isCompleted) {
        Habit habit = getHabitById(habitId);
        if (habit == null)
            return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMs);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long targetDateMs = cal.getTimeInMillis();

        long start = DateUtils.getStartOfDayMs(new Date(dateMs));
        long end = DateUtils.getEndOfDayMs(new Date(dateMs));

        if (!isCompleted) {
            // Or better check count
            habitCompletionDao.deleteByHabitIdAndDateRange(habitId, start, end);

            // Sync Today if needed
            if (DateUtils.isSameDay(new Date(targetDateMs), new Date())) {
                habit.setCurrentProgress(0);
                habit.setLastProgressDateMs(System.currentTimeMillis());
                // Reset last completed date if it was today
                habit.setLastCompletedDate(null);
                habitDao.update(toHabitEntity(habit));
            }
        } else {
            // ADD
            habitCompletionDao.deleteByHabitIdAndDateRange(habitId, start, end); // Clean slate

            HabitCompletionEntity completion = new HabitCompletionEntity();
            completion.habit_id = habitId;
            completion.completion_date_ms = targetDateMs;
            completion.day_part = Converters.calculateDayPart(targetDateMs);
            completion.adjusted_date_ms = Converters.calculateAdjustedDate(targetDateMs, 0);

            int count = (habit.isGoalTracking() && habit.getTargetCount() > 1) ? habit.getTargetCount() : 1;
            completion.count = count;
            completion.is_completed = true;

            habitCompletionDao.insert(completion);

            // Sync Today if needed
            if (DateUtils.isSameDay(new Date(targetDateMs), new Date())) {
                habit.setCurrentProgress(count);
                habit.setLastProgressDateMs(System.currentTimeMillis());
                habit.setLastCompletedDate(new Date(targetDateMs));
                habitDao.update(toHabitEntity(habit));
            }
        }

        streakManager.recalculateHabitStreaks(habitId);
        streakManager.recalculateOverallStreak();
    }
}
