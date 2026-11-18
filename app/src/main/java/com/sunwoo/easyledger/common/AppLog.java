package com.sunwoo.easyledger.common;

import android.util.Log;

public final class AppLog {
    private AppLog() {
    }

    public static void error(String tag, String action, Throwable error) {
        Log.e(tag, action + ": " + (error == null ? "unknown" : error.getClass().getSimpleName()));
    }
}
