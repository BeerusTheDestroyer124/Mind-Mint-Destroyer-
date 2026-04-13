package com.gxdevs.mindmint.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "habit_completions", indices = { @Index("habit_id"), @Index("completion_date_ms"),
        @Index("adjusted_date_ms") })
public class HabitCompletionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String habit_id;

    public long completion_date_ms; // Timestamp of when habit was completed

    // ============ NEW FIELDS FOR ENHANCED HABITS ============

    /**
     * Auto-tagged day part: MORNING (5-12), AFTERNOON (12-17), EVENING (17-21),
     * LATE_NIGHT (21-5)
     */
    public String day_part;

    /**
     * The "logical day" timestamp (considering habit's reset_time_hour).
     * If reset_time is 4 AM and user taps at 2 AM, this will be yesterday's date.
     */
    public long adjusted_date_ms;

    /**
     * Completion count for multi-tap habits (default 1).
     * For "8 glasses of water", each tap can add 1 or more.
     */
    public int count;

    /**
     * Optional note for this completion.
     */
    public String note;

    /**
     * Optional emotion for this completion.
     */
    public String emotion;

    /**
     * Whether the habit is actually "completed" or just "in progress".
     * Default true for backward compatibility.
     */
    public boolean is_completed = true;

    // Default constructor for Room
    public HabitCompletionEntity() {
        this.count = 1; // Default to 1 completion
    }

    @Ignore
    public HabitCompletionEntity(@NonNull String habit_id, long completion_date_ms) {
        this.habit_id = habit_id;
        this.completion_date_ms = completion_date_ms;
        this.count = 1;
    }

    @Ignore
    public HabitCompletionEntity(@NonNull String habit_id, long completion_date_ms,
            String day_part, long adjusted_date_ms, int count) {
        this.habit_id = habit_id;
        this.completion_date_ms = completion_date_ms;
        this.day_part = day_part;
        this.adjusted_date_ms = adjusted_date_ms;
        this.count = count;
    }
}
