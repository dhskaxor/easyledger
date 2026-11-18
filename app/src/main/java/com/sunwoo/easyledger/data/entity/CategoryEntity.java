package com.sunwoo.easyledger.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "categories",
        foreignKeys = {
                @ForeignKey(
                        entity = CategoryEntity.class,
                        parentColumns = "id",
                        childColumns = "parent_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("parent_id"), @Index("type")}
)
public class CategoryEntity {
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
    @ColumnInfo(name = "display_order")
    public int displayOrder;
    @ColumnInfo(name = "is_hidden")
    public boolean hidden;
    @ColumnInfo(name = "is_favorite")
    public boolean favorite;
    @ColumnInfo(name = "quick_amount_1")
    public long quickAmount1;
    @ColumnInfo(name = "quick_amount_2")
    public long quickAmount2;
    @ColumnInfo(name = "quick_amount_3")
    public long quickAmount3;
    @ColumnInfo(name = "created_at")
    public long createdAt;
    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public java.util.List<Long> quickAmounts() {
        java.util.ArrayList<Long> amounts = new java.util.ArrayList<>(3);
        if (quickAmount1 > 0) {
            amounts.add(quickAmount1);
        }
        if (quickAmount2 > 0) {
            amounts.add(quickAmount2);
        }
        if (quickAmount3 > 0) {
            amounts.add(quickAmount3);
        }
        return amounts;
    }
}
