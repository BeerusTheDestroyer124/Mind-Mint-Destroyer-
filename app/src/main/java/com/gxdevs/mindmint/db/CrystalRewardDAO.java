package com.gxdevs.mindmint.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.gxdevs.mindmint.Models.CrystalReward;

import java.util.List;

/**
 * Data Access Object for Crystal Rewards
 */
@Dao
public interface CrystalRewardDAO {
    
    @Insert
    void insertReward(CrystalReward reward);
    
    @Update
    void updateReward(CrystalReward reward);
    
    @Delete
    void deleteReward(CrystalReward reward);
    
    @Query("SELECT * FROM crystal_rewards ORDER BY timestamp DESC")
    List<CrystalReward> getAllRewards();
    
    @Query("SELECT * FROM crystal_rewards WHERE isCollected = 0 ORDER BY timestamp DESC")
    List<CrystalReward> getUncollectedRewards();
    
    @Query("SELECT SUM(crystalsEarned) FROM crystal_rewards WHERE isCollected = 1")
    int getTotalCrystalsCollected();
    
    @Query("SELECT * FROM crystal_rewards WHERE rewardType = :type ORDER BY timestamp DESC")
    List<CrystalReward> getRewardsByType(String type);
    
    @Query("SELECT * FROM crystal_rewards WHERE rarity = :rarity ORDER BY timestamp DESC")
    List<CrystalReward> getRewardsByRarity(String rarity);
}
