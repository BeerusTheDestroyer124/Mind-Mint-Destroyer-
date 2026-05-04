package com.gxdevs.mindmint.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a recurring Focus Mode schedule.
 * e.g. "Every Mon, Wed, Fri at 09:00 for 60 minutes"
 */
@Entity(tableName = "focus_schedules")
public class FocusScheduleEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** User-facing label, defaults to "Focus Block" */
    public String label;

    /** 0-23 */
    public int startHour;

    /** 0-59 */
    public int startMinute;

    /** How long to run in minutes (10-180) */
    public int durationMinutes;

    /**
     * Comma-separated day abbreviations the schedule applies to.
     * e.g. "MON,WED,FRI"  — uses Calendar day names: MON TUE WED THU FRI SAT SUN
     */
    public String daysOfWeek;

    /** If true, use Locked-In (all-app block) rather than user-selected blocks */
    public int isLockedIn; // 0 or 1

    /** Soft-toggle without deleting the schedule */
    public int isEnabled; // 0 or 1
}
