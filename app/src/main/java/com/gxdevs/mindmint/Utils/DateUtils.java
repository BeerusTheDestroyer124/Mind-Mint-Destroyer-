package com.gxdevs.mindmint.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    /**
     * Get today's date string in yyyy-MM-dd format
     */
    public static String getTodayDateString() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * Get yesterday's date string in yyyy-MM-dd format
     */
    public static String getYesterdayDateString() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return DATE_FORMAT.format(cal.getTime());
    }

    /**
     * Get start of day timestamp (00:00:00)
     */
    public static long getStartOfDayMs(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get end of day timestamp (23:59:59.999)
     */
    public static long getEndOfDayMs(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    /**
     * Get start of day timestamp for today
     */
    public static long getStartOfTodayMs() {
        return getStartOfDayMs(new Date());
    }

    /**
     * Get end of day timestamp for today
     */
    public static long getEndOfTodayMs() {
        return getEndOfDayMs(new Date());
    }

    /**
     * Get start of day timestamp for a specific date string (yyyy-MM-dd)
     */
    public static long getStartOfDayMs(String dateString) {
        try {
            Date date = DATE_FORMAT.parse(dateString);
            return getStartOfDayMs(date);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get end of day timestamp for a specific date string (yyyy-MM-dd)
     */
    public static long getEndOfDayMs(String dateString) {
        try {
            Date date = DATE_FORMAT.parse(dateString);
            return getEndOfDayMs(date);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if two dates are on the same day
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Check if a date is yesterday
     */
    public static boolean isYesterday(Date date) {
        if (date == null) return false;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return isSameDay(date, cal.getTime());
    }

    /**
     * Get date string for N days ago
     */
    public static String getDateStringDaysAgo(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return DATE_FORMAT.format(cal.getTime());
    }

    /**
     * Get start date for a week (7 days ago from today)
     */
    public static String getWeekStartDateString() {
        return getDateStringDaysAgo(6); // Today + 6 days ago = 7 days total
    }

    /**
     * Get start date for a month (30 days ago from today)
     */
    public static String getMonthStartDateString() {
        return getDateStringDaysAgo(29); // Today + 29 days ago = 30 days total
    }
}

