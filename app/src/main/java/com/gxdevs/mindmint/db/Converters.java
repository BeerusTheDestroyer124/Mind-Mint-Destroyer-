package com.gxdevs.mindmint.db;

import java.util.Calendar;

/**
 * Utility class for habit-related calculations.
 * Note: Room TypeConverters are not needed here since we store enums as
 * Strings.
 */
public class Converters {

    // Day part constants
    public static final String DAY_PART_MORNING = "MORNING"; // 5:00 - 11:59
    public static final String DAY_PART_AFTERNOON = "AFTERNOON"; // 12:00 - 16:59
    public static final String DAY_PART_EVENING = "EVENING"; // 17:00 - 20:59
    public static final String DAY_PART_LATE_NIGHT = "LATE_NIGHT"; // 21:00 - 4:59

    // Frequency goal constants
    public static final String FREQUENCY_DAILY = "DAILY";
    public static final String FREQUENCY_TIMES_PER_WEEK = "TIMES_PER_WEEK";
    public static final String FREQUENCY_UNLIMITED = "UNLIMITED";

    // Priority constants
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";

    /**
     * Calculate which day part a timestamp falls into.
     *
     * @param timestampMs The timestamp in milliseconds
     * @return One of: MORNING (5-12), AFTERNOON (12-17), EVENING (17-21),
     *         LATE_NIGHT (21-5)
     */
    public static String calculateDayPart(long timestampMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMs);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12)
            return DAY_PART_MORNING;
        if (hour >= 12 && hour < 17)
            return DAY_PART_AFTERNOON;
        if (hour >= 17 && hour < 21)
            return DAY_PART_EVENING;
        return DAY_PART_LATE_NIGHT; // 21:00 - 04:59
    }

    /**
     * Calculate the "adjusted date" based on the habit's reset hour.
     * If the current hour is before the reset hour, the logical day is "yesterday".
     *
     * Example: If reset_hour is 4 (4:00 AM):
     * - 3:00 AM completion → adjusted to previous day
     * - 5:00 AM completion → adjusted to current day
     *
     * @param timestampMs The timestamp in milliseconds
     * @param resetHour   The hour when the day resets (24h format: 0-23)
     * @return Start of the logical day in milliseconds (midnight of adjusted date)
     */
    public static long calculateAdjustedDate(long timestampMs, int resetHour) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMs);

        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        if (currentHour < resetHour) {
            // Before reset hour, this completion belongs to "yesterday"
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Normalize to start of logical day (midnight)
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    /**
     * Get the start of the current week (Sunday at midnight).
     *
     * @param timestampMs The reference timestamp
     * @return Start of week in milliseconds
     */
    public static long getWeekStartMs(long timestampMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMs);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get the end of the current week (Saturday at 23:59:59.999).
     *
     * @param timestampMs The reference timestamp
     * @return End of week in milliseconds
     */
    public static long getWeekEndMs(long timestampMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMs);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    /**
     * Format milliseconds into a human-readable interval.
     *
     * @param ms Interval in milliseconds
     * @return String like "26 hours", "3 days", "1.5 hours"
     */
    public static String formatInterval(long ms) {
        if (ms < 0)
            return "N/A";

        long hours = ms / (1000 * 60 * 60);
        if (hours < 24) {
            return hours + " hour" + (hours != 1 ? "s" : "");
        }

        long days = hours / 24;
        if (days < 7) {
            return days + " day" + (days != 1 ? "s" : "");
        }

        long weeks = days / 7;
        return weeks + " week" + (weeks != 1 ? "s" : "");
    }

    /**
     * Get day of week name from calendar day constant (0 = Sunday).
     */
    public static String getDayOfWeekName(int dayOfWeek) {
        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
        if (dayOfWeek >= 0 && dayOfWeek < 7) {
            return days[dayOfWeek];
        }
        return "Unknown";
    }
}
