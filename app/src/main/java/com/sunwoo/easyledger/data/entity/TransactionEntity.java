package com.sunwoo.easyledger.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "transactions",
        foreignKeys = {
                @ForeignKey(
                        entity = CategoryEntity.class,
                        parentColumns = "id",
                        childColumns = "category_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index("transaction_date"),
                @Index("category_id"),
                @Index("type"),
                @Index("fixed_transaction_id")
        }
)
public class TransactionEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String type;
    public long amount;
    @NonNull
    @ColumnInfo(name = "transaction_date")
    public String transactionDate;
    @NonNull
    @ColumnInfo(name = "transaction_time")
    public String transactionTime;
    @NonNull
    @ColumnInfo(name = "category_id")
    public String categoryId;
    @NonNull
    @ColumnInfo(name = "payment_method")
    public String paymentMethod;
    @NonNull
    public String memo;
    @ColumnInfo(name = "fixed_transaction_id")
    public String fixedTransactionId;
    @ColumnInfo(name = "occurrence_key")
    public String occurrenceKey;
    @ColumnInfo(name = "is_fixed")
    public boolean fixed;
    @ColumnInfo(name = "created_at")
    public long createdAt;
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
