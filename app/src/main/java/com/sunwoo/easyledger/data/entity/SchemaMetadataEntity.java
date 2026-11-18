package com.sunwoo.easyledger.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "schema_metadata")
public class SchemaMetadataEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "key")
    public String key;
    @NonNull
    public String value;
}
