package com.sunwoo.easyledger.common;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.sunwoo.easyledger.R;

public final class SheetDialogs {
    public static final float HEIGHT_COMPACT = 0.72f;
    public static final float HEIGHT_DEFAULT = 0.82f;
    public static final float HEIGHT_TALL = 0.88f;

    private SheetDialogs() {
    }

    public static int theme() {
        return R.style.Theme_EasyLedger_Dialog;
    }

    public static void apply(@Nullable DialogFragment fragment, float heightRatio) {
        if (fragment == null) {
            return;
        }
        Dialog dialog = fragment.getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null || fragment.getContext() == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Resources resources = fragment.requireContext().getResources();
        float ratio = Math.max(0.5f, Math.min(heightRatio, 0.95f));
        int height = (int) (resources.getDisplayMetrics().heightPixels * ratio);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
        window.setGravity(Gravity.BOTTOM);
        window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );
    }
}
