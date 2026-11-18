package com.sunwoo.easyledger.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.sunwoo.easyledger.data.entity.SchemaMetadataEntity;

@Dao
public interface SchemaMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void put(SchemaMetadataEntity entity);

    @Query("SELECT value FROM schema_metadata WHERE `key` = :key")
    String get(String key);
}
