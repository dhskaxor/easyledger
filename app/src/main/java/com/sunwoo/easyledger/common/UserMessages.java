package com.sunwoo.easyledger.common;

import com.sunwoo.easyledger.domain.model.UserVisibleException;

public final class UserMessages {
    public static final String SAVE_FAILED = "저장에 실패했습니다. 다시 시도해 주세요.";
    public static final String LOAD_FAILED = "데이터를 불러오지 못했습니다.";
    public static final String GENERIC = "요청을 처리하지 못했습니다. 다시 시도해 주세요.";
    public static final String STORAGE = "저장 공간이 부족하거나 파일을 열 수 없습니다.";

    private UserMessages() {
    }

    public static String from(Throwable error) {
        if (error instanceof UserVisibleException && error.getMessage() != null) {
            return error.getMessage();
        }
        Throwable cause = error == null ? null : error.getCause();
        if (cause instanceof UserVisibleException && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return GENERIC;
    }
}
