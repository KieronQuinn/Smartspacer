package com.google.android.gsa.overlay.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class FitsSystemWindowsNotifierFrameLayout extends FrameLayout {

    public OnApplySystemWindowsCallback iGw;

    public FitsSystemWindowsNotifierFrameLayout(Context context) {
        super(context);
    }

    public FitsSystemWindowsNotifierFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public FitsSystemWindowsNotifierFrameLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @TargetApi(21)
    public FitsSystemWindowsNotifierFrameLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    protected boolean fitSystemWindows(Rect rect) {
        if (this.iGw != null) {
            return this.iGw.onApplySystemWindows(rect);
        }
        return false;
    }
}
