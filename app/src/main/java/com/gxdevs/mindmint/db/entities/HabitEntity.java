package com.gxdevs.mindmint.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habits")
public class HabitEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String reason;
    public String difficulty;
    public String duration_type;
    public int duration_days;
    /**
     * Name of the drawable resource for the habit icon (e.g., "flame", "droplets").
     *
     * Storing the name instead of the raw int ID makes the value stable across
     * builds and drawable reordering. The app will resolve this name back to a
     * safe drawable at runtime with a fallback to a default icon if missing.
     */
    public String icon_name;
    public int icon_tint; // color for icon tint
    public int icon_background_tint; // color for icon background tint
    public long created_at_ms;
    public long last_completed_date_ms;
    public int current_streak_days;
    public int max_streak_days;
    public int milestone_index;

    // ============ NEW FIELDS FOR GOAL TRACKING (Migration 9->10) ============
    public boolean is_goal_tracking;
    public int one_tap_value;
    public int current_progress;
    public long last_progress_date_ms;

    // ============ NEW FIELDS FOR ENHANCED HABITS ============

    /**
     * Frequency goal type: DAILY, TIMES_PER_WEEK, UNLIMITED
     * - DAILY: Must complete every day (considering reset_time_hour)
     * - TIMES_PER_WEEK: Must complete X times per week (resets Sunday)
     * - UNLIMITED: Counter mode, no streak (just milestones)
     */
    public String frequency_goal;

    /**
     * Number of times per week (used when frequency_goal = TIMES_PER_WEEK)
     */
    public int frequency_times_per_week;

    /**
     * Hour when the "day" resets (24h format: 0-23)
     * Example: 4 = 4:00 AM, so a 3 AM completion counts for "yesterday"
     */
    public int reset_time_hour;

    /**
     * Target completions per cycle (e.g., 8 for "8 glasses of water")
     */
    public int target_count;

    /**
     * Unit label for the target (e.g., "glasses", "pages", "reps")
     */
    public String target_unit;

    /**
     * Priority level: LOW, MEDIUM, HIGH
     */
    public String priority;

    /**
     * How many days can be skipped per week without breaking streak.
     * For DAILY habits with allowed_skips_per_week = 2, user can miss 2 days/week.
     */
    public int allowed_skips_per_week;

    public boolean is_ask_emotion;
}
