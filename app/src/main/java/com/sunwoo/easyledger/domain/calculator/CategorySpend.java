package com.sunwoo.easyledger.domain.calculator;

public final class CategorySpend {
    public final String categoryId;
    public final String parentId;
    public final String name;
    public final String parentName;
    public final long amount;

    public CategorySpend(String categoryId, String parentId, String name, String parentName, long amount) {
        this.categoryId = categoryId;
        this.parentId = parentId;
        this.name = name;
        this.parentName = parentName;
        this.amount = amount;
    }
}
