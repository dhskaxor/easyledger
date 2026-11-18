package com.sunwoo.easyledger.domain.model;

import com.sunwoo.easyledger.domain.calculator.SearchSummary;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchCriteriaTest {
    @Test
    public void categoriesUseOrAndOtherFieldsUseAnd() {
        SearchCriteria criteria = new SearchCriteria(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                new HashSet<>(Arrays.asList("food", "cafe")),
                "점심",
                1000L,
                20000L,
                TransactionType.EXPENSE.name(),
                PaymentMethod.CARD.name()
        );
        assertTrue(criteria.matches(item("food", 8000, "회사 점심", PaymentMethod.CARD.name())));
        assertTrue(criteria.matches(item("cafe", 4500, "점심 커피", PaymentMethod.CARD.name())));
        assertFalse(criteria.matches(item("bus", 1500, "점심", PaymentMethod.CARD.name())));
        assertFalse(criteria.matches(item("food", 8000, "점심", PaymentMethod.CASH.name())));
        assertFalse(criteria.matches(item("food", 500, "점심", PaymentMethod.CARD.name())));
        SearchSummary summary = SearchSummary.from(Collections.singletonList(
                item("food", 8000, "회사 점심", PaymentMethod.CARD.name())
        ));
        assertEquals(1, summary.count);
        assertEquals(8000L, summary.expense);
        assertEquals(0L, summary.income);
    }

    @Test
    public void emptyCategorySetDoesNotFilter() {
        SearchCriteria criteria = new SearchCriteria(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                Collections.emptySet(),
                "",
                null,
                null,
                null,
                null
        );
        assertTrue(criteria.matches(item("food", 1000, "", PaymentMethod.CASH.name())));
        assertEquals("%100\\%%", SearchCriteria.likePattern("100%"));
    }

    private static Searchable item(String category, long amount, String memo, String payment) {
        return new Searchable(
                TransactionType.EXPENSE.name(),
                amount,
                LocalDate.of(2026, 11, 16),
                category,
                payment,
                memo
        );
    }
}
