package com.sunwoo.easyledger.data.database;

import com.sunwoo.easyledger.domain.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

/** 온보딩·시드용 기본 카테고리 목록. */
public final class LibraryItem {
    public final String id;
    public final String name;
    public final String type;
    public final String parentId;
    public final String icon;
    public final boolean defaultEnabled;
    public final boolean favorite;

    public LibraryItem(
            String id,
            String name,
            String type,
            String parentId,
            String icon,
            boolean defaultEnabled,
            boolean favorite
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.parentId = parentId;
        this.icon = icon;
        this.defaultEnabled = defaultEnabled;
        this.favorite = favorite;
    }

    private static LibraryItem expense(String id, String name, String icon) {
        return new LibraryItem(id, name, TransactionType.EXPENSE.name(), "", icon, true, false);
    }

    private static LibraryItem income(String id, String name, String icon) {
        return new LibraryItem(id, name, TransactionType.INCOME.name(), "", icon, true, false);
    }

    public static LibraryItem[] all() {
        return new LibraryItem[] {
                expense("exp_food", "식비", "food"),
                expense("exp_dining", "외식", "dining"),
                expense("exp_cafe", "카페·간식", "cafe"),
                expense("exp_delivery", "배달음식", "dining"),
                expense("exp_shopping", "쇼핑", "shopping"),
                expense("exp_clothes", "의류·잡화", "shopping"),
                expense("exp_digital", "가전·디지털", "subscription"),
                expense("exp_transport", "교통", "bus"),
                expense("exp_fuel", "주유·주차", "fuel"),
                expense("exp_taxi", "택시·대리", "taxi"),
                expense("exp_car", "차량유지", "transport"),
                expense("exp_housing", "주거", "home"),
                expense("exp_telecom", "통신", "phone"),
                expense("exp_living", "생활용품", "living"),
                expense("exp_health", "건강·의료", "health"),
                expense("exp_beauty", "미용", "beauty"),
                expense("exp_culture", "문화·여가", "culture"),
                expense("exp_sports", "운동", "sports"),
                expense("exp_travel", "여행·숙박", "travel"),
                expense("exp_education", "교육", "education"),
                expense("exp_child", "육아", "baby"),
                expense("exp_pet", "반려동물", "pet"),
                expense("exp_subscription", "구독", "subscription"),
                expense("exp_finance", "보험·금융", "finance"),
                expense("exp_loan", "대출·이자", "finance"),
                expense("exp_tax", "세금·공과금", "bill"),
                expense("exp_transfer", "이체·송금", "income"),
                expense("exp_event", "경조사", "event"),
                expense("exp_donation", "기부", "heart"),
                expense("exp_saving", "저축·투자", "saving"),
                expense("exp_etc", "기타", "more"),
                income("inc_salary", "급여", "salary"),
                income("inc_bonus", "상여", "bonus"),
                income("inc_side", "부수입", "side"),
                income("inc_business", "사업소득", "side"),
                income("inc_allowance", "용돈", "allowance"),
                income("inc_interest", "이자·배당", "interest"),
                income("inc_refund", "환급·보험금", "refund"),
                income("inc_sale", "판매수입", "shopping"),
                income("inc_etc", "기타 수입", "income")
        };
    }

    public static List<LibraryItem> roots(String type) {
        List<LibraryItem> rows = new ArrayList<>();
        for (LibraryItem item : all()) {
            if ((item.parentId == null || item.parentId.isEmpty()) && item.type.equals(type)) {
                rows.add(item);
            }
        }
        return rows;
    }
}
