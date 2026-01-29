package com.gxdevs.mindmint.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Pre-computed stats for faster UI rendering.
 * Updated on tap (hybrid caching) + periodic refresh.
 */
@Entity(tableName = "habit_cached_stats")
public class HabitCachedStatsEntity {
    @PrimaryKey
    @NonNull
    public String habit_id;

    // ============ CORE STATS ============

    /**
     * Total completions ever (for milestones like "You meditated 100 times!")
     */
    public int total_completions;

    /**
     * Average time between completions in milliseconds.
     * Display: "You perform this every 26 hours"
     */
    public long average_interval_ms;

    /**
     * Longest time without doing the habit in milliseconds.
     * Display: "Your longest break was 5 days; try to keep it under 2!"
     */
    public long longest_break_ms;

    /**
     * Completions this week (for TIMES_PER_WEEK habits)
     */
    public int current_week_count;

    /**
     * Skips used this week (for allowed_skips tracking)
     */
    public int skips_used_this_week;

    // ============ PATTERN ANALYSIS (JSON) ============

    /**
     * Completions by day of week as JSON.
     * Format: {"0": 15, "1": 12, "2": 8, "3": 10, "4": 14, "5": 20, "6": 18}
     * (0 = Sunday, 6 = Saturday)
     * Display: "You rarely do this habit on Sundays"
     */
    public String day_of_week_heatmap;

    /**
     * Completions by day part as JSON.
     * Format: {"MORNING": 45, "AFTERNOON": 30, "EVENING": 15, "LATE_NIGHT": 10}
     */
    public String day_part_distribution;

    // ============ WEEK-OVER-WEEK COMPARISON ============

    /**
     * Total completions this week
     */
    public int this_week_volume;

    /**
     * Total completions last week
     */
    public int last_week_volume;

    /**
     * Volume change percentage.
     * Display: "You've increased your water intake by 20% compared to last week"
     */
    public float volume_change_percent;

    // ============ DOMINANT PATTERN ============

    /**
     * The day part with most completions (MORNING, AFTERNOON, EVENING, LATE_NIGHT)
     */
    public String dominant_day_part;

    /**
     * Percentage of completions in the dominant day part.
     * Display: "You're a Morning Person for this habit (80% of taps before noon)"
     */
    public float dominant_day_part_percent;

    // ============ CACHE METADATA ============

    /**
     * Last time these stats were recalculated
     */
    public long last_updated_ms;
}
