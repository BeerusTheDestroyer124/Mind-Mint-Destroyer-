package com.gxdevs.mindmint.Models;

import java.util.Date;

public class Habit {
    private String id;
    private String name;
    private String reason;
    private Difficulty difficulty;

    private DurationType durationType;
    private int durationDays;

    private int icon; // drawable resource ID for the habit icon
    private int iconTint; // color for icon tint
    private int iconBackgroundTint; // color for icon background tint

    private Date createdAt;
    private Date lastCompletedDate; // for daily check
    private int currentStreakDays;
    private int maxStreakDays; // maximum streak achieved

    // Progress milestones index: 0=3d,1=1w,2=2w,3=1m,4=3m
    private int milestoneIndex;

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    private boolean isAskEmotion;

    public enum DurationType {
        CHALLENGE_N_DAYS,
        INDEFINITE,
        UNTIL_GOAL
    }

    public Habit() {
        this.id = generateId();
        this.createdAt = new Date();
        this.difficulty = Difficulty.MEDIUM;
        this.durationType = DurationType.INDEFINITE;
        this.durationDays = 0;
        this.currentStreakDays = 0;
        this.maxStreakDays = 0;
        this.milestoneIndex = 0;
        this.icon = 0; // 0 means no icon set
        this.iconTint = 0;
        this.iconBackgroundTint = 0;
    }

    public Habit(String name, String reason, Difficulty difficulty) {
        this();
        this.name = name;
        this.reason = reason;
        this.difficulty = difficulty;
    }

    private String generateId() {
        return "habit_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public DurationType getDurationType() {
        return durationType;
    }

    public void setDurationType(DurationType durationType) {
        this.durationType = durationType;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getCurrentStreakDays() {
        return currentStreakDays;
    }

    public void setCurrentStreakDays(int currentStreakDays) {
        this.currentStreakDays = currentStreakDays;
    }

    public int getMaxStreakDays() {
        return maxStreakDays;
    }

    public void setMaxStreakDays(int maxStreakDays) {
        this.maxStreakDays = maxStreakDays;
    }

    public int getMilestoneIndex() {
        return milestoneIndex;
    }

    public void setMilestoneIndex(int milestoneIndex) {
        this.milestoneIndex = milestoneIndex;
    }

    public Date getLastCompletedDate() {
        return lastCompletedDate;
    }

    public void setLastCompletedDate(Date lastCompletedDate) {
        this.lastCompletedDate = lastCompletedDate;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getIconTint() {
        return iconTint;
    }

    public void setIconTint(int iconTint) {
        this.iconTint = iconTint;
    }

    public int getIconBackgroundTint() {
        return iconBackgroundTint;
    }

    public void setIconBackgroundTint(int iconBackgroundTint) {
        this.iconBackgroundTint = iconBackgroundTint;
    }

    public boolean isDoneToday() {
        if (lastCompletedDate == null)
            return false;

        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar last = java.util.Calendar.getInstance();
        last.setTime(lastCompletedDate);

        return now.get(java.util.Calendar.YEAR) == last.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) == last.get(java.util.Calendar.DAY_OF_YEAR);
    }

    public void markDoneToday() {
        Date today = new Date();
        if (!isDoneToday()) {
            currentStreakDays += 1;
        }
        lastCompletedDate = today;
    }

    // Decrease progress when user unchecks today's completion
    public void unmarkToday() {
        if (isDoneToday()) {
            if (currentStreakDays > 0)
                currentStreakDays -= 1;
            // Reset lastCompletedDate to null to reflect not done today
            lastCompletedDate = null;
        }
    }

    private String formatTargetLabel(int days) {
        if (days <= 0)
            return "";
        if (days < 7)
            return days + "d";
        if (days % 7 == 0 && days < 28) {
            int weeks = days / 7;
            return weeks + "w";
        }
        // Special-case common waypoints
        if (days == 28)
            return "4w";
        if (days == 45)
            return "1.5m";
        // Months approximation
        if (days % 30 == 0) {
            int months = days / 30;
            return months + "m";
        }
        // Fallback: round to one decimal month representation
        double monthsApprox = days / 30.0;
        double rounded = Math.round(monthsApprox * 2.0) / 2.0; // nearest .5
        if (Math.abs(rounded - Math.floor(rounded)) < 1e-6) {
            return ((int) rounded) + "m";
        } else {
            return rounded + "m";
        }
    }

    /**
     * Get streak description text based on the current streak count
     * 
     * @return A motivational description string based on streak days
     */
    public String getStreakDescription() {
        int streak = currentStreakDays;

        if (streak == 0) {
            return "Start your journey";
        } else if (streak == 1) {
            return "Great start!";
        } else if (streak < 3) {
            return "Keep it going";
        } else if (streak < 7) {
            return "Building momentum";
        } else if (streak < 14) {
            return "Keep the flame alive";
        } else if (streak < 30) {
            return "You're on fire!";
        } else if (streak < 60) {
            return "Unstoppable!";
        } else if (streak < 90) {
            return "Legend in the making";
        } else if (streak < 180) {
            return "Master of consistency";
        } else if (streak < 365) {
            return "Almost a year strong!";
        } else if (streak == 365) {
            return "A full year! Incredible!";
        } else if (streak < 730) {
            return "Beyond a year! Amazing!";
        } else {
            return "True dedication!";
        }
    }

    public boolean isAskEmotion() {
        return isAskEmotion;
    }

    public void setAskEmotion(boolean askEmotion) {
        isAskEmotion = askEmotion;
    }

    // ============ GOAL TRACKING FIELDS ============

    private boolean isGoalTracking;
    private int oneTapValue = 1;
    private int currentProgress;
    private long lastProgressDateMs;
    private int targetCount = 1;
    private String targetUnit;

    public boolean isGoalTracking() {
        return isGoalTracking;
    }

    public void setGoalTracking(boolean goalTracking) {
        isGoalTracking = goalTracking;
    }

    public int getOneTapValue() {
        return oneTapValue;
    }

    public void setOneTapValue(int oneTapValue) {
        this.oneTapValue = oneTapValue;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public long getLastProgressDateMs() {
        return lastProgressDateMs;
    }

    public void setLastProgressDateMs(long lastProgressDateMs) {
        this.lastProgressDateMs = lastProgressDateMs;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int targetCount) {
        this.targetCount = targetCount;
    }

    public String getTargetUnit() {
        return targetUnit;
    }

    public void setTargetUnit(String targetUnit) {
        this.targetUnit = targetUnit;
    }

    /**
     * Resets progress if the last progress update was not today.
     * 
     * @return true if reset happened
     */
    public boolean resetProgressIfNeeded() {
        if (lastProgressDateMs == 0)
            return false;

        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar last = java.util.Calendar.getInstance();
        last.setTimeInMillis(lastProgressDateMs);

        boolean isToday = now.get(java.util.Calendar.YEAR) == last.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) == last.get(java.util.Calendar.DAY_OF_YEAR);

        if (!isToday) {
            currentProgress = 0;
            // logic: we don't update lastProgressDateMs here immediately,
            // only when user interacts and sets new progress.
            return true;
        }
        return false;
    }
}
