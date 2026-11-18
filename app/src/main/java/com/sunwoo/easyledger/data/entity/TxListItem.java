package com.sunwoo.easyledger.data.entity;

import androidx.room.ColumnInfo;

public class TxListItem {
    public String id;
    public String type;
    public long amount;
    @ColumnInfo(name = "transaction_date")
    public String transactionDate;
    @ColumnInfo(name = "transaction_time")
    public String transactionTime;
    @ColumnInfo(name = "category_id")
    public String categoryId;
    @ColumnInfo(name = "category_name")
    public String categoryName;
    @ColumnInfo(name = "category_icon")
    public String categoryIcon;
    @ColumnInfo(name = "payment_method")
    public String paymentMethod;
    public String memo;
    @ColumnInfo(name = "is_fixed")
    public boolean fixed;
}
