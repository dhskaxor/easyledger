package com.sunwoo.easyledger.common;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class WindowInsetsHelper {
    private WindowInsetsHelper() {
    }

    public static void applySystemBars(View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom
            );
            // Already padded here — stop children (e.g. BottomNavigationView) from padding again.
            return insets.inset(bars.left, bars.top, bars.right, bars.bottom);
        });
        ViewCompat.requestApplyInsets(root);
    }
}
