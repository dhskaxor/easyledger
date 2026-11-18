package com.sunwoo.easyledger.common;

public final class SettingKeys {
    public static final String DEFAULT_PAYMENT = "default_payment_method";
    public static final String INCLUDE_FIXED = "statistics_include_fixed";
    public static final String ONBOARDING_DONE = "onboarding_done";
    public static final String LIBRARY_PREFIX = "library_activated_";
    public static final String SAVED_SEARCH_FILTERS = "saved_search_filters";
    public static final String REMINDER_NO_ENTRY = "reminder_no_entry";
    public static final String REMINDER_FIXED = "reminder_fixed";

    private SettingKeys() {
    }

    public static String libraryKey(String libraryId) {
        return LIBRARY_PREFIX + libraryId;
    }
}
