package com.sunwoo.easyledger.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sunwoo.easyledger.data.entity.FixedOccurrenceEntity;

import java.util.List;

@Dao
public interface FixedOccurrenceDao {
    @Insert
    void insert(FixedOccurrenceEntity entity);

    @Query("SELECT occurrence_date FROM fixed_occurrences WHERE fixed_transaction_id = :fixedId")
    List<String> dates(String fixedId);

    @Query("UPDATE fixed_occurrences SET status = 'DELETED', transaction_id = NULL "
            + "WHERE transaction_id = :transactionId")
    void markDeletedByTransaction(String transactionId);

    @Query("SELECT * FROM fixed_occurrences ORDER BY occurrence_date, id")
    List<FixedOccurrenceEntity> all();

    @Query("DELETE FROM fixed_occurrences")
    void deleteAll();
}
