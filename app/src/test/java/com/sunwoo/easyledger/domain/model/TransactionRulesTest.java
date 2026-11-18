package com.sunwoo.easyledger.domain.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TransactionRulesTest {
    @Test
    public void requiresPositiveAmountMatchingCategoryAndLimitsMemo() {
        assertNull(TransactionRules.validate(
                "EXPENSE", 1200, "2026-11-16", "21:05:00", "food", "EXPENSE", "CASH", "김밥"
        ));
        assertEquals("금액은 0보다 커야 합니다.", TransactionRules.validate(
                "EXPENSE", 0, "2026-11-16", "21:05:00", "food", "EXPENSE", "CASH", ""
        ));
        assertEquals("카테고리와 거래 유형이 맞지 않습니다.", TransactionRules.validate(
                "INCOME", 1000, "2026-11-16", "21:05:00", "food", "EXPENSE", "CARD", ""
        ));
        assertEquals("메모는 200자까지 입력할 수 있습니다.", TransactionRules.validate(
                "EXPENSE", 1000, "2026-11-16", "21:05:00", "food", "EXPENSE", "CASH", "가".repeat(201)
        ));
        assertEquals("날짜가 올바르지 않습니다.", TransactionRules.validate(
                "EXPENSE", 1000, "2026-02-31", "21:05:00", "food", "EXPENSE", "CASH", ""
        ));
    }
}
