package com.sunwoo.easyledger.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_settings")
public class AppSettingEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "key")
    public String key;
    @NonNull
    public String value;
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
