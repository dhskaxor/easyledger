package com.sunwoo.easyledger.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sunwoo.easyledger.data.entity.FixedTransactionEntity;

import java.util.List;

@Dao
public interface FixedTransactionDao {
    @Insert
    void insert(FixedTransactionEntity entity);

    @Update
    void update(FixedTransactionEntity entity);

    @Query("SELECT * FROM fixed_transactions WHERE id = :id")
    FixedTransactionEntity get(String id);

    @Query("SELECT * FROM fixed_transactions WHERE is_active = 1 ORDER BY name")
    List<FixedTransactionEntity> active();

    @Query("SELECT * FROM fixed_transactions ORDER BY is_active DESC, name")
    LiveData<List<FixedTransactionEntity>> observeAll();

    @Query("SELECT * FROM fixed_transactions ORDER BY name")
    List<FixedTransactionEntity> all();

    @Query("DELETE FROM fixed_transactions")
    void deleteAll();
}
