package com.gxdevs.mindmint.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.gxdevs.mindmint.db.entities.FocusTopicEntity;

import java.util.List;

@Dao
public interface FocusTopicDao {
    @Insert
    void insert(FocusTopicEntity topic);

    @Delete
    void delete(FocusTopicEntity topic);

    @Query("SELECT * FROM focus_topics")
    LiveData<List<FocusTopicEntity>> getAllTopics();

    @Query("SELECT * FROM focus_topics")
    List<FocusTopicEntity> getAllTopicsSync();

    @Query("SELECT COUNT(*) FROM focus_topics")
    int getCount();

    @Query("DELETE FROM focus_topics")
    void deleteAll();
}
