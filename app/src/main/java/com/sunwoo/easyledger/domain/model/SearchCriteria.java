package com.sunwoo.easyledger.domain.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SearchCriteria {
    public final LocalDate start;
    public final LocalDate end;
    public final Set<String> categoryIds;
    public final String memoKeyword;
    public final Long minAmount;
    public final Long maxAmount;
    public final String type;
    public final String payment;

    public SearchCriteria(
            LocalDate start,
            LocalDate end,
            Set<String> categoryIds,
            String memoKeyword,
            Long minAmount,
            Long maxAmount,
            String type,
            String payment
    ) {
        this.start = start;
        this.end = end;
        this.categoryIds = categoryIds == null ? Collections.emptySet() : new HashSet<>(categoryIds);
        this.memoKeyword = memoKeyword == null ? "" : memoKeyword.trim();
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.type = type == null || type.isEmpty() ? null : type;
        this.payment = payment == null || payment.isEmpty() ? null : payment;
    }

    public boolean matches(Searchable item) {
        if (item.date.isBefore(start) || item.date.isAfter(end)) {
            return false;
        }
        if (!categoryIds.isEmpty() && !categoryIds.contains(item.categoryId)) {
            return false;
        }
        if (!memoKeyword.isEmpty()) {
            String memo = item.memo == null ? "" : item.memo;
            if (!memo.toLowerCase(Locale.ROOT).contains(memoKeyword.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (minAmount != null && item.amount < minAmount) {
            return false;
        }
        if (maxAmount != null && item.amount > maxAmount) {
            return false;
        }
        if (type != null && !type.equals(item.type)) {
            return false;
        }
        return payment == null || payment.equals(item.payment);
    }

    public static String likePattern(String keyword) {
        String escaped = keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
