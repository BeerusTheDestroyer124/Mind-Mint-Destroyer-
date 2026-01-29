package com.gxdevs.mindmint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gxdevs.mindmint.db.entities.DailyStatsEntity;

import java.util.List;

@Dao
public interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    DailyStatsEntity getByDate(String date);

    @Query("SELECT * FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    List<DailyStatsEntity> getByDateRange(String startDate, String endDate);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(DailyStatsEntity entity);

    @Update
    void update(DailyStatsEntity entity);

    @Query("DELETE FROM daily_stats WHERE date = :date")
    void deleteByDate(String date);

    @Query("DELETE FROM daily_stats")
    void deleteAll();

    @Query("SELECT * FROM daily_stats")
    List<DailyStatsEntity> getAll();
}
