package com.gxdevs.mindmint.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gxdevs.mindmint.db.entities.FocusScheduleEntity;

import java.util.List;

@Dao
public interface FocusScheduleDao {

    @Query("SELECT * FROM focus_schedules ORDER BY startHour ASC, startMinute ASC")
    List<FocusScheduleEntity> getAll();

    @Query("SELECT * FROM focus_schedules WHERE isEnabled = 1 ORDER BY startHour ASC, startMinute ASC")
    List<FocusScheduleEntity> getEnabled();

    @Query("SELECT * FROM focus_schedules WHERE id = :id LIMIT 1")
    FocusScheduleEntity getById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FocusScheduleEntity schedule);

    @Update
    void update(FocusScheduleEntity schedule);

    @Delete
    void delete(FocusScheduleEntity schedule);

    @Query("DELETE FROM focus_schedules WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT COUNT(*) FROM focus_schedules")
    int count();
}
