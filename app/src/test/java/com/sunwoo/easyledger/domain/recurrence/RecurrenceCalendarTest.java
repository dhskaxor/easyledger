package com.sunwoo.easyledger.domain.recurrence;

import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.sunwoo.easyledger.domain.model.RecurrenceType;
import com.sunwoo.easyledger.domain.model.TransactionType;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecurrenceCalendarTest {
    @Test
    public void monthlyThirtyFirstClampsAndRestores() {
        List<LocalDate> dates = RecurrenceCalendar.dates(
                RecurrenceType.MONTHLY,
                "31",
                LocalDate.of(2023, 1, 31),
                LocalDate.of(2023, 1, 31),
                LocalDate.of(2023, 5, 31)
        );
        assertEquals(Arrays.asList(
                LocalDate.of(2023, 1, 31),
                LocalDate.of(2023, 2, 28),
                LocalDate.of(2023, 3, 31),
                LocalDate.of(2023, 4, 30),
                LocalDate.of(2023, 5, 31)
        ), dates);
    }

    @Test
    public void leapYearFebruaryTwentyNinth() {
        List<LocalDate> dates = RecurrenceCalendar.dates(
                RecurrenceType.MONTHLY,
                "31",
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 3, 31)
        );
        assertEquals(LocalDate.of(2024, 2, 29), dates.get(1));
    }

    @Test
    public void yearlyLeapDayFallsBackInCommonYears() {
        List<LocalDate> dates = RecurrenceCalendar.dates(
                RecurrenceType.YEARLY,
                "02-29",
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2028, 2, 29)
        );
        assertEquals(Arrays.asList(
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2025, 2, 28),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2028, 2, 29)
        ), dates);
    }

    @Test
    public void weeklyIncludesWeekendsAndSkipsExisting() {
        LocalDate start = LocalDate.of(2026, 1, 3);
        FixedRule rule = rule(start, RecurrenceType.WEEKLY, "SATURDAY", null, true, start);
        List<LocalDate> missing = RecurrenceCalendar.missing(
                rule,
                LocalDate.of(2026, 1, 24),
                new HashSet<>(Collections.singleton(LocalDate.of(2026, 1, 10)))
        );
        assertEquals(Arrays.asList(
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 17),
                LocalDate.of(2026, 1, 24)
        ), missing);
        assertEquals(DayOfWeekName.SATURDAY, start.getDayOfWeek().name());
    }

    @Test
    public void dailyStopsAtEndAndDoesNotCreateFuture() {
        LocalDate start = LocalDate.of(2026, 11, 14);
        FixedRule rule = rule(start, RecurrenceType.DAILY, "1", LocalDate.of(2026, 11, 16), true, start);
        List<LocalDate> missing = RecurrenceCalendar.missing(rule, LocalDate.of(2026, 11, 20), Collections.emptySet());
        assertEquals(Arrays.asList(
                LocalDate.of(2026, 11, 14),
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 16)
        ), missing);
    }

    @Test
    public void inactiveAndEffectiveFromDoNotBackfillGap() {
        FixedRule inactive = rule(
                LocalDate.of(2026, 1, 1),
                RecurrenceType.DAILY,
                "1",
                null,
                false,
                LocalDate.of(2026, 1, 1)
        );
        assertTrue(RecurrenceCalendar.missing(inactive, LocalDate.of(2026, 1, 5), Collections.emptySet()).isEmpty());

        FixedRule resumed = rule(
                LocalDate.of(2026, 1, 1),
                RecurrenceType.DAILY,
                "1",
                null,
                true,
                LocalDate.of(2026, 1, 4)
        );
        List<LocalDate> dates = RecurrenceCalendar.missing(resumed, LocalDate.of(2026, 1, 5), Collections.emptySet());
        assertEquals(Arrays.asList(LocalDate.of(2026, 1, 4), LocalDate.of(2026, 1, 5)), dates);
        assertFalse(dates.contains(LocalDate.of(2026, 1, 3)));
    }

    private static FixedRule rule(
            LocalDate start,
            RecurrenceType type,
            String value,
            LocalDate end,
            boolean active,
            LocalDate effective
    ) {
        return new FixedRule(
                "rule",
                TransactionType.EXPENSE.name(),
                "월세",
                500000,
                "cat",
                PaymentMethod.CARD.name(),
                type,
                value,
                start,
                end,
                active,
                effective,
                1
        );
    }

    private static final class DayOfWeekName {
        private static final String SATURDAY = "SATURDAY";
    }
}
