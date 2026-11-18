package com.sunwoo.easyledger.domain.recurrence;

import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.sunwoo.easyledger.domain.model.RecurrenceType;
import com.sunwoo.easyledger.domain.model.TransactionType;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuleChangePlanTest {
    @Test
    public void newActiveRuleFillsFromStart() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        RuleChangePlan plan = RuleChangePlan.forNew(active(start, 1000, start));
        assertFalse(plan.backfillOldThroughYesterday);
        assertTrue(plan.generateThroughToday);
        assertEquals(1, plan.ruleVersion);
        assertEquals(start, plan.effectiveFrom);
    }

    @Test
    public void amountChangeKeepsPastRuleUntilToday() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        LocalDate today = LocalDate.of(2026, 11, 16);
        FixedRule oldRule = active(start, 1000, start);
        FixedRule updated = copy(oldRule, 2000, true);
        RuleChangePlan plan = RuleChangePlan.forUpdate(oldRule, updated, today);
        assertTrue(plan.backfillOldThroughYesterday);
        assertTrue(plan.generateThroughToday);
        assertEquals(2, plan.ruleVersion);
        assertEquals(today, plan.effectiveFrom);
    }

    @Test
    public void deactivationBackfillsButDoesNotGenerateToday() {
        LocalDate today = LocalDate.of(2026, 11, 16);
        FixedRule oldRule = active(LocalDate.of(2026, 1, 1), 1000, LocalDate.of(2026, 1, 1));
        RuleChangePlan plan = RuleChangePlan.forUpdate(oldRule, copy(oldRule, 1000, false), today);
        assertTrue(plan.backfillOldThroughYesterday);
        assertFalse(plan.generateThroughToday);
    }

    @Test
    public void reactivationDoesNotFillInactiveGap() {
        LocalDate today = LocalDate.of(2026, 11, 16);
        FixedRule paused = copy(active(LocalDate.of(2026, 1, 1), 1000, LocalDate.of(2026, 3, 1)), 1000, false);
        RuleChangePlan plan = RuleChangePlan.forUpdate(paused, copy(paused, 1000, true), today);
        assertFalse(plan.backfillOldThroughYesterday);
        assertTrue(plan.generateThroughToday);
        assertEquals(today, plan.effectiveFrom);
    }

    @Test
    public void nameOnlyChangeDoesNotTouchGeneratedTransactions() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        FixedRule oldRule = active(start, 1000, start);
        FixedRule renamed = new FixedRule(
                oldRule.id, oldRule.type, "새 이름", oldRule.amount, oldRule.categoryId,
                oldRule.paymentMethod, oldRule.recurrenceType, oldRule.recurrenceValue,
                oldRule.startDate, oldRule.endDate, true, oldRule.effectiveFrom, oldRule.ruleVersion
        );
        RuleChangePlan plan = RuleChangePlan.forUpdate(oldRule, renamed, LocalDate.of(2026, 11, 16));
        assertFalse(plan.backfillOldThroughYesterday);
        assertFalse(plan.generateThroughToday);
        assertEquals(oldRule.ruleVersion, plan.ruleVersion);
        assertEquals(oldRule.effectiveFrom, plan.effectiveFrom);
    }

    private static FixedRule active(LocalDate start, long amount, LocalDate effective) {
        return new FixedRule(
                "id",
                TransactionType.INCOME.name(),
                "급여",
                amount,
                "salary",
                PaymentMethod.CASH.name(),
                RecurrenceType.MONTHLY,
                "1",
                start,
                null,
                true,
                effective,
                1
        );
    }

    private static FixedRule copy(FixedRule rule, long amount, boolean active) {
        return new FixedRule(
                rule.id, rule.type, rule.name, amount, rule.categoryId, rule.paymentMethod,
                rule.recurrenceType, rule.recurrenceValue, rule.startDate, rule.endDate,
                active, rule.effectiveFrom, rule.ruleVersion
        );
    }
}
