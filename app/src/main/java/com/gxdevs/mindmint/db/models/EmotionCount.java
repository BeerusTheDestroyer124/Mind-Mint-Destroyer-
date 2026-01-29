package com.gxdevs.mindmint.db.models;

/**
 * POJO for emotion aggregation query results.
 */
public class EmotionCount {
    /**
     * Emotion string (e.g., "Proud", "Satisfied", etc.)
     */
    public String emotion;

    /**
     * Count of this emotion
     */
    public int count;
}
