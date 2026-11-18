package com.sunwoo.easyledger.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category_library")
public class CategoryLibraryEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String name;
    @NonNull
    public String type;
    @ColumnInfo(name = "parent_id")
    public String parentId;
    @NonNull
    public String icon;
    @ColumnInfo(name = "default_enabled")
    public boolean defaultEnabled;
}
