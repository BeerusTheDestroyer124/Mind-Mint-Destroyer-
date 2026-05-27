package com.gxdevs.mindmint.Models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

/**
 * CrystalReward - Model for tracking crystal rewards earned by user
 */
@Entity(tableName = "crystal_rewards")
public class CrystalReward implements Serializable {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int crystalsEarned;
    private int totalCrystals;
    private String rewardType; // "focus_completion", "daily_streak", "app_blocking", "habit_completion"
    private String description;
    private long timestamp;
    private boolean isCollected;
    private String rarity; // "common", "rare", "epic", "legendary"
    
    public CrystalReward() {}
    
    public CrystalReward(int crystalsEarned, String rewardType, String description, String rarity) {
        this.crystalsEarned = crystalsEarned;
        this.rewardType = rewardType;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
        this.isCollected = false;
        this.rarity = rarity;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getCrystalsEarned() {
        return crystalsEarned;
    }
    
    public void setCrystalsEarned(int crystalsEarned) {
        this.crystalsEarned = crystalsEarned;
    }
    
    public int getTotalCrystals() {
        return totalCrystals;
    }
    
    public void setTotalCrystals(int totalCrystals) {
        this.totalCrystals = totalCrystals;
    }
    
    public String getRewardType() {
        return rewardType;
    }
    
    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isCollected() {
        return isCollected;
    }
    
    public void setCollected(boolean collected) {
        isCollected = collected;
    }
    
    public String getRarity() {
        return rarity;
    }
    
    public void setRarity(String rarity) {
        this.rarity = rarity;
    }
    
    /**
     * Get color for rarity level
     */
    public int getRarityColor() {
        switch(rarity.toLowerCase()) {
            case "legendary":
                return 0xFFFFD700; // Gold
            case "epic":
                return 0xFF9932CC; // Purple
            case "rare":
                return 0xFF1E90FF; // Blue
            case "common":
            default:
                return 0xFF808080; // Gray
        }
    }
}
