package com.gxdevs.mindmint.db.models;

/**
 * POJO for day part aggregation query results.
 */
public class DayPartCount {
    /**
     * Day part: MORNING, AFTERNOON, EVENING, LATE_NIGHT
     */
    public String day_part;

    /**
     * Count of completions in this day part
     */
    public int count;
}
