package com.sunwoo.easyledger.data.entity;

import androidx.room.ColumnInfo;

public class CategorySum {
    @ColumnInfo(name = "category_id")
    public String categoryId;
    public long total;
}
