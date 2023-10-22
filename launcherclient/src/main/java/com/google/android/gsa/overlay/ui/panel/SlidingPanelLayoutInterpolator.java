package com.google.android.gsa.overlay.ui.panel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.animation.Interpolator;

final class SlidingPanelLayoutInterpolator extends AnimatorListenerAdapter implements Interpolator {

    private ObjectAnimator mAnimator;
    int mFinalX;
    private final SlidingPanelLayout slidingPanelLayout;

    public SlidingPanelLayoutInterpolator(SlidingPanelLayout slidingPanelLayoutVar) {
        this.slidingPanelLayout = slidingPanelLayoutVar;
    }

    public final void cnP() {
        if (this.mAnimator != null) {
            this.mAnimator.removeAllListeners();
            this.mAnimator.cancel();
        }
    }

    public final void dt(int i, int i2) {
        cnP();
        this.mFinalX = i;
        if (i2 > 0) {
            this.mAnimator = ObjectAnimator.ofInt(this.slidingPanelLayout, SlidingPanelLayout.PANEL_X, i).setDuration((long) i2);
            this.mAnimator.setInterpolator(this);
            this.mAnimator.addListener(this);
            this.mAnimator.start();
            return;
        }
        onAnimationEnd(null);
    }

    public final boolean isFinished() {
        return this.mAnimator == null;
    }

    public final void onAnimationEnd(Animator animator) {
        this.mAnimator = null;
        this.slidingPanelLayout.BM(this.mFinalX);
        SlidingPanelLayout slidingPanelLayoutVar = this.slidingPanelLayout;
        if (slidingPanelLayoutVar.mSettling) {
            slidingPanelLayoutVar.mSettling = false;
            if (slidingPanelLayoutVar.panelX == 0) {
                slidingPanelLayoutVar.cnO();
                slidingPanelLayoutVar.mIsPanelOpen = false;
                slidingPanelLayoutVar.mIsPageMoving = false;
                if (slidingPanelLayoutVar.dragCallback != null) {
                    slidingPanelLayoutVar.dragCallback.onPanelClose();
                }
            } else if (slidingPanelLayoutVar.panelX == slidingPanelLayoutVar.getMeasuredWidth()) {
                slidingPanelLayoutVar.cnG();
            }
        }
    }

    public final float getInterpolation(float f) {
        float f2 = f - 1.0f;
        return (f2 * (((f2 * f2) * f2) * f2)) + 1.0f;
    }
}