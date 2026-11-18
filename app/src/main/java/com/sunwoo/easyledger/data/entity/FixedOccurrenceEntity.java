package com.sunwoo.easyledger.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "fixed_occurrences",
        foreignKeys = {
                @ForeignKey(
                        entity = FixedTransactionEntity.class,
                        parentColumns = "id",
                        childColumns = "fixed_transaction_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = TransactionEntity.class,
                        parentColumns = "id",
                        childColumns = "transaction_id",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index(value = {"fixed_transaction_id", "occurrence_date"}, unique = true),
                @Index("transaction_id")
        }
)
public class FixedOccurrenceEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    @ColumnInfo(name = "fixed_transaction_id")
    public String fixedTransactionId;
    @NonNull
    @ColumnInfo(name = "occurrence_date")
    public String occurrenceDate;
    @ColumnInfo(name = "transaction_id")
    public String transactionId;
    @NonNull
    public String status;
    @ColumnInfo(name = "rule_version")
    public int ruleVersion;
    @ColumnInfo(name = "created_at")
    public long createdAt;
}
