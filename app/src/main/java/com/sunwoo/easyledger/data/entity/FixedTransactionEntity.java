package com.sunwoo.easyledger.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "fixed_transactions",
        foreignKeys = {
                @ForeignKey(
                        entity = CategoryEntity.class,
                        parentColumns = "id",
                        childColumns = "category_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("category_id")}
)
public class FixedTransactionEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String type;
    @NonNull
    public String name;
    public long amount;
    @NonNull
    @ColumnInfo(name = "category_id")
    public String categoryId;
    @NonNull
    @ColumnInfo(name = "payment_method")
    public String paymentMethod;
    @NonNull
    @ColumnInfo(name = "recurrence_type")
    public String recurrenceType;
    @NonNull
    @ColumnInfo(name = "recurrence_value")
    public String recurrenceValue;
    @NonNull
    @ColumnInfo(name = "start_date")
    public String startDate;
    @ColumnInfo(name = "end_date")
    public String endDate;
    @ColumnInfo(name = "is_active")
    public boolean active;
    @NonNull
    @ColumnInfo(name = "effective_from")
    public String effectiveFrom;
    @ColumnInfo(name = "rule_version")
    public int ruleVersion;
    @ColumnInfo(name = "created_at")
    public long createdAt;
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
