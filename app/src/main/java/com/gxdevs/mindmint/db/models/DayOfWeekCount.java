package com.gxdevs.mindmint.db.models;

/**
 * POJO for day of week aggregation query results.
 */
public class DayOfWeekCount {
    /**
     * Day of week as string (0-6, where 0 = Sunday)
     */
    public String day_of_week;

    /**
     * Count of completions on this day
     */
    public int count;
}
