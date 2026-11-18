package com.sunwoo.easyledger.domain.recurrence;

import java.time.LocalDate;

/**
 * 반복 규칙 변경 시 과거 거래를 지키면서 누락분을 만드는 정책.
 * 금액이나 일정이 바뀌면 적용일(오늘) 이전의 미생성분만 기존 규칙으로 만들고,
 * 오늘부터는 새 규칙을 쓴다. 비활성 구간은 나중에 다시 켜도 소급 생성하지 않는다.
 */
public final class RuleChangePlan {
    public final boolean backfillOldThroughYesterday;
    public final boolean generateThroughToday;
    public final int ruleVersion;
    public final LocalDate effectiveFrom;

    public RuleChangePlan(
            boolean backfillOldThroughYesterday,
            boolean generateThroughToday,
            int ruleVersion,
            LocalDate effectiveFrom
    ) {
        this.backfillOldThroughYesterday = backfillOldThroughYesterday;
        this.generateThroughToday = generateThroughToday;
        this.ruleVersion = ruleVersion;
        this.effectiveFrom = effectiveFrom;
    }

    public static RuleChangePlan forNew(FixedRule rule) {
        return new RuleChangePlan(false, rule.active, 1, rule.startDate);
    }

    public static RuleChangePlan forUpdate(FixedRule oldRule, FixedRule newRule, LocalDate today) {
        boolean generatedFieldsChanged = !oldRule.sameGeneratedFields(newRule);
        boolean activeChanged = oldRule.active != newRule.active;
        if (!generatedFieldsChanged && !activeChanged) {
            return new RuleChangePlan(false, false, oldRule.ruleVersion, oldRule.effectiveFrom);
        }
        boolean backfill = oldRule.active && (generatedFieldsChanged || !newRule.active);
        return new RuleChangePlan(
                backfill,
                newRule.active,
                oldRule.ruleVersion + 1,
                today
        );
    }
}
