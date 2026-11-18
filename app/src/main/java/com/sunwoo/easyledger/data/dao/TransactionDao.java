package com.sunwoo.easyledger.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sunwoo.easyledger.data.entity.CategorySum;
import com.sunwoo.easyledger.data.entity.DayTotal;
import com.sunwoo.easyledger.data.entity.IncomeExpense;
import com.sunwoo.easyledger.data.entity.TransactionEntity;
import com.sunwoo.easyledger.data.entity.TxListItem;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(TransactionEntity entity);

    @Update
    void update(TransactionEntity entity);

    @Query("SELECT * FROM transactions WHERE id = :id")
    TransactionEntity get(String id);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM transactions")
    int count();

    @Query("SELECT COUNT(*) FROM transactions WHERE transaction_date = :date")
    int countOnDate(String date);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND transaction_date = :date")
    long expenseOnDate(String date);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND transaction_date = :date")
    long incomeOnDate(String date);

    @Query("SELECT * FROM transactions ORDER BY transaction_date, transaction_time, id")
    List<TransactionEntity> all();

    @Query("SELECT t.id AS id, t.type AS type, t.amount AS amount, "
            + "t.transaction_date AS transaction_date, t.transaction_time AS transaction_time, "
            + "t.category_id AS category_id, c.name AS category_name, c.icon AS category_icon, "
            + "t.payment_method AS payment_method, t.memo AS memo, t.is_fixed AS is_fixed "
            + "FROM transactions t INNER JOIN categories c ON c.id = t.category_id "
            + "WHERE t.transaction_date >= :start AND t.transaction_date <= :end "
            + "AND (:useType = 0 OR t.type = :type) "
            + "AND (:usePayment = 0 OR t.payment_method = :payment) "
            + "AND (:useMin = 0 OR t.amount >= :minAmount) "
            + "AND (:useMax = 0 OR t.amount <= :maxAmount) "
            + "AND (:useMemo = 0 OR IFNULL(t.memo, '') LIKE :memoPattern ESCAPE '\\') "
            + "AND (:allCategories = 1 OR t.category_id IN (:categoryIds)) "
            + "ORDER BY t.transaction_date DESC, t.transaction_time DESC, t.created_at DESC")
    LiveData<List<TxListItem>> observeSearch(
            String start,
            String end,
            int useType,
            String type,
            int usePayment,
            String payment,
            int useMin,
            long minAmount,
            int useMax,
            long maxAmount,
            int useMemo,
            String memoPattern,
            int allCategories,
            List<String> categoryIds
    );

    @Query("SELECT t.id AS id, t.type AS type, t.amount AS amount, "
            + "t.transaction_date AS transaction_date, t.transaction_time AS transaction_time, "
            + "t.category_id AS category_id, c.name AS category_name, c.icon AS category_icon, "
            + "t.payment_method AS payment_method, t.memo AS memo, t.is_fixed AS is_fixed "
            + "FROM transactions t INNER JOIN categories c ON c.id = t.category_id "
            + "WHERE t.transaction_date = :date "
            + "ORDER BY t.transaction_time ASC, t.created_at ASC")
    LiveData<List<TxListItem>> observeDay(String date);

    @Query("SELECT t.id AS id, t.type AS type, t.amount AS amount, "
            + "t.transaction_date AS transaction_date, t.transaction_time AS transaction_time, "
            + "t.category_id AS category_id, c.name AS category_name, c.icon AS category_icon, "
            + "t.payment_method AS payment_method, t.memo AS memo, t.is_fixed AS is_fixed "
            + "FROM transactions t INNER JOIN categories c ON c.id = t.category_id "
            + "WHERE t.transaction_date = :date "
            + "ORDER BY t.transaction_time DESC, t.created_at DESC")
    List<TxListItem> listOnDate(String date);

    @Query("SELECT transaction_date AS date, "
            + "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income, "
            + "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense "
            + "FROM transactions WHERE transaction_date >= :start AND transaction_date <= :end "
            + "GROUP BY transaction_date")
    LiveData<List<DayTotal>> observeDayTotals(String start, String end);

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income, "
            + "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense "
            + "FROM transactions WHERE transaction_date >= :start AND transaction_date <= :end "
            + "AND (:includeFixed = 1 OR is_fixed = 0)")
    LiveData<IncomeExpense> observeRange(String start, String end, int includeFixed);

    @Query("SELECT transaction_date AS date, "
            + "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income, "
            + "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense "
            + "FROM transactions WHERE transaction_date >= :start AND transaction_date <= :end "
            + "AND (:includeFixed = 1 OR is_fixed = 0) GROUP BY transaction_date")
    LiveData<List<DayTotal>> observeFilteredDays(String start, String end, int includeFixed);

    @Query("SELECT category_id AS category_id, SUM(amount) AS total FROM transactions "
            + "WHERE type = 'EXPENSE' AND transaction_date >= :start AND transaction_date <= :end "
            + "AND (:includeFixed = 1 OR is_fixed = 0) GROUP BY category_id")
    LiveData<List<CategorySum>> observeCategoryExpense(String start, String end, int includeFixed);
}
