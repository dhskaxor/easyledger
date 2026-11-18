package com.sunwoo.easyledger.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.sunwoo.easyledger.data.entity.AppSettingEntity;

import java.util.List;

@Dao
public interface AppSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void put(AppSettingEntity entity);

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    String get(String key);

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    LiveData<String> observe(String key);

    @Query("SELECT * FROM app_settings ORDER BY `key`")
    List<AppSettingEntity> all();

    @Query("DELETE FROM app_settings")
    void deleteAll();
}
