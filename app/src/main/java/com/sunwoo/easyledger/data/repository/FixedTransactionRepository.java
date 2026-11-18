package com.sunwoo.easyledger.data.repository;

import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.data.dao.CategoryDao;
import com.sunwoo.easyledger.data.dao.FixedOccurrenceDao;
import com.sunwoo.easyledger.data.dao.FixedTransactionDao;
import com.sunwoo.easyledger.data.dao.TransactionDao;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.FixedOccurrenceEntity;
import com.sunwoo.easyledger.data.entity.FixedTransactionEntity;
import com.sunwoo.easyledger.data.entity.TransactionEntity;
import com.sunwoo.easyledger.domain.model.Limits;
import com.sunwoo.easyledger.domain.model.OccurrenceStatus;
import com.sunwoo.easyledger.domain.model.TransactionRules;
import com.sunwoo.easyledger.domain.model.UserVisibleException;
import com.sunwoo.easyledger.domain.recurrence.FixedRule;
import com.sunwoo.easyledger.domain.recurrence.RecurrenceCalendar;
import com.sunwoo.easyledger.domain.recurrence.RuleChangePlan;
import com.sunwoo.easyledger.domain.model.RecurrenceType;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FixedTransactionRepository {
    public static final Object LOCK = new Object();

    private final EasyLedgerDatabase database;
    private final FixedTransactionDao fixedDao;
    private final FixedOccurrenceDao occurrenceDao;
    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;

    public FixedTransactionRepository(EasyLedgerDatabase database) {
        this.database = database;
        fixedDao = database.fixedTransactionDao();
        occurrenceDao = database.fixedOccurrenceDao();
        transactionDao = database.transactionDao();
        categoryDao = database.categoryDao();
    }

    public androidx.lifecycle.LiveData<List<FixedTransactionEntity>> observeAll() {
        return fixedDao.observeAll();
    }

    public FixedTransactionEntity get(String id) {
        return fixedDao.get(id);
    }

    public List<FixedTransactionEntity> activeRules() {
        return fixedDao.active();
    }

    public int generateMissing(LocalDate today) {
        synchronized (LOCK) {
            int created = 0;
            for (FixedTransactionEntity entity : fixedDao.active()) {
                try {
                    FixedRule rule = toRule(entity);
                    created += database.runInTransaction(() -> insertMissing(rule, today));
                } catch (Exception exception) {
                    com.sunwoo.easyledger.common.AppLog.error("FixedTransaction", "generate", exception);
                }
            }
            return created;
        }
    }

    public void create(FixedRule draft) throws UserVisibleException {
        validate(draft);
        synchronized (LOCK) {
            RuleChangePlan plan = RuleChangePlan.forNew(draft);
            FixedRule stored = draft.withVersion(plan.ruleVersion, plan.effectiveFrom);
            database.runInTransaction(() -> {
                fixedDao.insert(toEntity(stored, System.currentTimeMillis(), System.currentTimeMillis()));
                if (plan.generateThroughToday) {
                    insertMissing(stored, LocalDate.now());
                }
            });
        }
    }

    public void update(FixedRule draft) throws UserVisibleException {
        validate(draft);
        synchronized (LOCK) {
            database.runInTransaction(() -> {
                FixedTransactionEntity existing = fixedDao.get(draft.id);
                if (existing == null) {
                    throw new UserVisibleException("반복 거래를 찾을 수 없습니다.");
                }
                FixedRule oldRule = toRule(existing);
                LocalDate today = LocalDate.now();
                RuleChangePlan plan = RuleChangePlan.forUpdate(oldRule, draft, today);
                if (plan.backfillOldThroughYesterday) {
                    insertMissing(oldRule, today.minusDays(1));
                }
                FixedRule stored = draft.withVersion(plan.ruleVersion, plan.effectiveFrom);
                FixedTransactionEntity entity = toEntity(stored, existing.createdAt, System.currentTimeMillis());
                fixedDao.update(entity);
                if (plan.generateThroughToday) {
                    insertMissing(stored, today);
                }
            });
        }
    }

    public static FixedRule toRule(FixedTransactionEntity entity) {
        return new FixedRule(
                entity.id,
                entity.type,
                entity.name,
                entity.amount,
                entity.categoryId,
                entity.paymentMethod,
                RecurrenceType.fromStorage(entity.recurrenceType),
                entity.recurrenceValue,
                LocalDate.parse(entity.startDate),
                entity.endDate == null || entity.endDate.isEmpty() ? null : LocalDate.parse(entity.endDate),
                entity.active,
                LocalDate.parse(entity.effectiveFrom),
                entity.ruleVersion
        );
    }

    private void validate(FixedRule draft) throws UserVisibleException {
        CategoryEntity category = categoryDao.get(draft.categoryId);
        String error = com.sunwoo.easyledger.domain.model.FixedRuleValidator.validate(
                draft.type,
                draft.name,
                draft.amount,
                draft.categoryId,
                category == null ? null : category.type,
                draft.paymentMethod,
                draft.recurrenceType.name(),
                draft.recurrenceValue,
                draft.startDate.toString(),
                draft.endDate == null ? "" : draft.endDate.toString()
        );
        if (error != null) {
            throw new UserVisibleException(error);
        }
    }

    private int insertMissing(FixedRule rule, LocalDate horizon) throws UserVisibleException {
        CategoryEntity category = categoryDao.get(rule.categoryId);
        if (category == null) {
            throw new UserVisibleException("반복 거래의 카테고리를 찾을 수 없습니다.");
        }
        Set<LocalDate> existing = new HashSet<>();
        for (String date : occurrenceDao.dates(rule.id)) {
            existing.add(LocalDate.parse(date));
        }
        int created = 0;
        long now = System.currentTimeMillis();
        for (LocalDate date : RecurrenceCalendar.missing(rule, horizon, existing)) {
            String transactionId = UUID.randomUUID().toString();
            TransactionEntity transaction = new TransactionEntity();
            transaction.id = transactionId;
            transaction.type = rule.type;
            transaction.amount = rule.amount;
            transaction.transactionDate = date.toString();
            transaction.transactionTime = DateTimes.formatTime(Limits.FIXED_TRANSACTION_TIME);
            transaction.categoryId = rule.categoryId;
            transaction.paymentMethod = rule.paymentMethod;
            transaction.memo = "";
            transaction.fixedTransactionId = rule.id;
            transaction.occurrenceKey = rule.id + "|" + date;
            transaction.fixed = true;
            transaction.createdAt = now;
            transaction.updatedAt = now;
            String ruleError = TransactionRules.validate(
                    transaction.type,
                    transaction.amount,
                    transaction.transactionDate,
                    transaction.transactionTime,
                    transaction.categoryId,
                    category.type,
                    transaction.paymentMethod,
                    transaction.memo
            );
            if (ruleError != null) {
                throw new UserVisibleException(ruleError);
            }
            transactionDao.insert(transaction);
            FixedOccurrenceEntity occurrence = new FixedOccurrenceEntity();
            occurrence.id = UUID.randomUUID().toString();
            occurrence.fixedTransactionId = rule.id;
            occurrence.occurrenceDate = date.toString();
            occurrence.transactionId = transactionId;
            occurrence.status = OccurrenceStatus.GENERATED.name();
            occurrence.ruleVersion = rule.ruleVersion;
            occurrence.createdAt = now;
            occurrenceDao.insert(occurrence);
            created++;
        }
        return created;
    }

    private static FixedTransactionEntity toEntity(FixedRule rule, long createdAt, long updatedAt) {
        FixedTransactionEntity entity = new FixedTransactionEntity();
        entity.id = rule.id;
        entity.type = rule.type;
        entity.name = rule.name.trim();
        entity.amount = rule.amount;
        entity.categoryId = rule.categoryId;
        entity.paymentMethod = rule.paymentMethod;
        entity.recurrenceType = rule.recurrenceType.name();
        entity.recurrenceValue = rule.recurrenceValue;
        entity.startDate = rule.startDate.toString();
        entity.endDate = rule.endDate == null ? null : rule.endDate.toString();
        entity.active = rule.active;
        entity.effectiveFrom = rule.effectiveFrom.toString();
        entity.ruleVersion = rule.ruleVersion;
        entity.createdAt = createdAt;
        entity.updatedAt = updatedAt;
        return entity;
    }
}
