package com.sunwoo.easyledger.data.repository;

import androidx.lifecycle.LiveData;

import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.data.dao.CategoryDao;
import com.sunwoo.easyledger.data.dao.FixedOccurrenceDao;
import com.sunwoo.easyledger.data.dao.TransactionDao;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.CategorySum;
import com.sunwoo.easyledger.data.entity.DayTotal;
import com.sunwoo.easyledger.data.entity.IncomeExpense;
import com.sunwoo.easyledger.data.entity.TransactionEntity;
import com.sunwoo.easyledger.data.entity.TxListItem;
import com.sunwoo.easyledger.domain.model.TransactionRules;
import com.sunwoo.easyledger.domain.model.UserVisibleException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TransactionRepository {
    private final EasyLedgerDatabase database;
    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;
    private final FixedOccurrenceDao occurrenceDao;

    public TransactionRepository(EasyLedgerDatabase database) {
        this.database = database;
        transactionDao = database.transactionDao();
        categoryDao = database.categoryDao();
        occurrenceDao = database.fixedOccurrenceDao();
    }

    public TransactionEntity get(String id) {
        return transactionDao.get(id);
    }

    public void insert(TransactionEntity entity) throws UserVisibleException {
        prepare(entity, true);
        transactionDao.insert(entity);
    }

    public void update(TransactionEntity entity) throws UserVisibleException {
        database.runInTransaction(() -> {
            TransactionEntity existing = transactionDao.get(entity.id);
            if (existing == null) {
                throw new UserVisibleException("거래를 찾을 수 없습니다.");
            }
            entity.createdAt = existing.createdAt;
            entity.fixed = existing.fixed;
            entity.fixedTransactionId = existing.fixedTransactionId;
            entity.occurrenceKey = existing.occurrenceKey;
            prepare(entity, false);
            transactionDao.update(entity);
        });
    }

    public void delete(String id) {
        database.runInTransaction(() -> {
            occurrenceDao.markDeletedByTransaction(id);
            transactionDao.deleteById(id);
        });
    }

    public String copy(String id) throws UserVisibleException {
        TransactionEntity existing = transactionDao.get(id);
        if (existing == null) {
            throw new UserVisibleException("거래를 찾을 수 없습니다.");
        }
        long now = System.currentTimeMillis();
        TransactionEntity copy = new TransactionEntity();
        copy.id = UUID.randomUUID().toString();
        copy.type = existing.type;
        copy.amount = existing.amount;
        copy.transactionDate = DateTimes.today().toString();
        copy.transactionTime = DateTimes.formatTime(DateTimes.now());
        copy.categoryId = existing.categoryId;
        copy.paymentMethod = existing.paymentMethod;
        copy.memo = existing.memo == null ? "" : existing.memo;
        copy.fixed = false;
        copy.fixedTransactionId = null;
        copy.occurrenceKey = null;
        copy.createdAt = now;
        copy.updatedAt = now;
        prepare(copy, false);
        transactionDao.insert(copy);
        return copy.id;
    }

    public LiveData<List<TxListItem>> observeSearch(
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
    ) {
        List<String> ids = categoryIds == null || categoryIds.isEmpty()
                ? Collections.singletonList("-")
                : categoryIds;
        return transactionDao.observeSearch(
                start, end, useType, type == null ? "" : type, usePayment, payment == null ? "" : payment,
                useMin, minAmount, useMax, maxAmount, useMemo, memoPattern, allCategories, ids
        );
    }

    public LiveData<List<TxListItem>> observeDay(String date) {
        return transactionDao.observeDay(date);
    }

    public LiveData<List<DayTotal>> observeDayTotals(String start, String end) {
        return transactionDao.observeDayTotals(start, end);
    }

    public LiveData<IncomeExpense> observeRange(String start, String end, boolean includeFixed) {
        return transactionDao.observeRange(start, end, includeFixed ? 1 : 0);
    }

    public LiveData<List<DayTotal>> observeFilteredDays(String start, String end, boolean includeFixed) {
        return transactionDao.observeFilteredDays(start, end, includeFixed ? 1 : 0);
    }

    public LiveData<List<CategorySum>> observeCategoryExpense(String start, String end, boolean includeFixed) {
        return transactionDao.observeCategoryExpense(start, end, includeFixed ? 1 : 0);
    }

    public List<TransactionEntity> all() {
        return transactionDao.all();
    }

    public int count() {
        return transactionDao.count();
    }

    public int countOnDate(String date) {
        return transactionDao.countOnDate(date);
    }

    public long expenseOnDate(String date) {
        return transactionDao.expenseOnDate(date);
    }

    public long incomeOnDate(String date) {
        return transactionDao.incomeOnDate(date);
    }

    public List<TxListItem> listOnDate(String date) {
        return transactionDao.listOnDate(date);
    }

    private void prepare(TransactionEntity entity, boolean fresh) throws UserVisibleException {
        if (entity.memo == null) {
            entity.memo = "";
        }
        CategoryEntity category = categoryDao.get(entity.categoryId);
        String error = TransactionRules.validate(
                entity.type,
                entity.amount,
                entity.transactionDate,
                entity.transactionTime,
                entity.categoryId,
                category == null ? null : category.type,
                entity.paymentMethod,
                entity.memo
        );
        if (error != null) {
            throw new UserVisibleException(error);
        }
        long now = System.currentTimeMillis();
        if (fresh || entity.id == null || entity.id.isEmpty()) {
            entity.id = UUID.randomUUID().toString();
            entity.createdAt = now;
            entity.fixed = false;
            entity.fixedTransactionId = null;
            entity.occurrenceKey = null;
        }
        entity.updatedAt = now;
        LocalDate.parse(entity.transactionDate);
        LocalTime.parse(entity.transactionTime);
    }
}
