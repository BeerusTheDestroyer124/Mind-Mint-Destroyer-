package com.gxdevs.mindmint.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_topics")
public class FocusTopicEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public FocusTopicEntity(String name) {
        this.name = name;
    }
}
