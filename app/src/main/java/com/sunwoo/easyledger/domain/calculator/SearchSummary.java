package com.sunwoo.easyledger.domain.calculator;

import com.sunwoo.easyledger.domain.model.Searchable;
import com.sunwoo.easyledger.domain.model.TransactionType;

import java.util.List;

public final class SearchSummary {
    public final int count;
    public final long income;
    public final long expense;

    public SearchSummary(int count, long income, long expense) {
        this.count = count;
        this.income = income;
        this.expense = expense;
    }

    public static SearchSummary from(List<Searchable> items) {
        long income = 0;
        long expense = 0;
        for (Searchable item : items) {
            if (TransactionType.INCOME.name().equals(item.type)) {
                income += item.amount;
            } else if (TransactionType.EXPENSE.name().equals(item.type)) {
                expense += item.amount;
            }
        }
        return new SearchSummary(items.size(), income, expense);
    }
}
