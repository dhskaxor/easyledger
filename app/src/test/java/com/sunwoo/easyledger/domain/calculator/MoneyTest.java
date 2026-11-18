package com.sunwoo.easyledger.domain.calculator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MoneyTest {
    @Test
    public void formatsThousandsWithoutDecimals() {
        assertEquals("0", Money.format(0));
        assertEquals("12,345", Money.format(12345));
        assertEquals("-1,000", Money.format(-1000));
        assertEquals("12,345원", Money.won(12345));
    }

    @Test
    public void keepsTwelveDigitsAndStripsLeadingZeros() {
        assertEquals("100", Money.appendDigits("1", "00"));
        assertEquals("0", Money.backspace("5"));
        assertEquals("999999999999", Money.appendDigits("999999999999", "1"));
        assertEquals(12000L, Money.toAmount("000012000"));
    }

    @Test
    public void rejectsDecimalAndCommaInStoredAmount() {
        assertThrows(IllegalArgumentException.class, () -> Money.parsePlain("1.5"));
        assertThrows(IllegalArgumentException.class, () -> Money.parsePlain("1,000"));
        assertEquals(1000L, Money.parsePlain("1000"));
    }
}
