package com.google.android.gsa.overlay.ui.panel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Property;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.google.android.gsa.overlay.base.SlidingPanelLayoutDragCallback;

public class SlidingPanelLayout extends FrameLayout {
    private static final boolean uoK = false;
    private static final boolean uoL = false;
    public static final Property PANEL_X = new SlidingPanelLayoutProperty(Integer.class, "panelX");
    public float mDownX;
    public float mDownY;
    public int mActivePointerId = -1;
    private final float mDensity;
    private final int mFlingThresholdVelocity;
    public boolean mIsPageMoving = false;
    public final boolean mIsRtl;
    private float mLastMotionX;
    private final int mMaximumVelocity;
    private final int mMinFlingVelocity;
    private final int mMinSnapVelocity;
    private float mTotalMotionX;
    public final int mTouchSlop;
    public int mTouchState = 0;
    private VelocityTracker mVelocityTracker;
    public View uoA;
    private View uoB;
    public int panelX;
    public float mPanelPositionRatio;
    private float uoE;
    private float uoF;
    private final SlidingPanelLayoutInterpolator slidingPanelLayoutInterpolator;
    public SlidingPanelLayoutDragCallback dragCallback;
    public boolean mIsPanelOpen = false;
    public boolean mForceDrag;
    public boolean mSettling;
    private final DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(3.0f);

    public SlidingPanelLayout(Context context) {
        super(context);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        this.mTouchSlop = viewConfiguration.getScaledPagingTouchSlop();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mDensity = getResources().getDisplayMetrics().density;
        this.mFlingThresholdVelocity = (int) (500.0f * this.mDensity);
        this.mMinFlingVelocity = (int) (250.0f * this.mDensity);
        this.mMinSnapVelocity = (int) (1500.0f * this.mDensity);
        this.slidingPanelLayoutInterpolator = new SlidingPanelLayoutInterpolator(this);
        this.mIsRtl = isRtl(getResources());
    }

    public final void el(View view) {
        this.uoA = view;
        super.addView(this.uoA);
    }

    public final void BM(int i) {
        if (i <= 1) {
            i = 0;
        }
        int measuredWidth = getMeasuredWidth();
        this.mPanelPositionRatio = ((float) i) / ((float) measuredWidth);
        this.panelX = Math.max(Math.min(i, measuredWidth), 0);
        this.uoA.setTranslationX(this.mIsRtl ? (float) (-this.panelX) : (float) this.panelX);
        if (uoK) {
            this.uoA.setAlpha(Math.max(0.1f, this.decelerateInterpolator.getInterpolation(this.mPanelPositionRatio)));
        }
        if (this.dragCallback != null) {
            this.dragCallback.onDragProgress(this.mPanelPositionRatio);
        }
    }

    public final void fv(int i) {
        cnF();
        this.mSettling = true;
        this.slidingPanelLayoutInterpolator.dt(getMeasuredWidth(), i);
    }

    public final void closePanel(int i) {
        this.mIsPageMoving = true;
        if (this.dragCallback != null) {
            boolean z;
            SlidingPanelLayoutDragCallback tVar = this.dragCallback;
            z = this.mTouchState == 1;
            tVar.onPanelClosing(z);
        }
        this.mSettling = true;
        this.slidingPanelLayoutInterpolator.dt(0, i);
    }

    public final void em(View view) {
        if (this.uoB != null) {
            super.removeView(this.uoB);
        }
        this.uoB = view;
        super.addView(this.uoB, 0);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        acquireVelocityTrackerAndAddMovement(motionEvent);
        if (getChildCount() <= 0) {
            return super.onInterceptTouchEvent(motionEvent);
        }
        int action = motionEvent.getAction();
        if (action == 2 && this.mTouchState == 1) {
            return true;
        }
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                boolean z;
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                this.mDownX = x;
                this.mDownY = y;
                this.uoF = (float) this.panelX;
                this.mLastMotionX = x;
                this.mTotalMotionX = 0.0f;
                this.mActivePointerId = motionEvent.getPointerId(0);
                action = Math.abs(this.slidingPanelLayoutInterpolator.mFinalX - this.panelX);
                z = this.slidingPanelLayoutInterpolator.isFinished() || action < this.mTouchSlop / 3;
                if (!z || this.mForceDrag) {
                    this.mForceDrag = false;
                    cnN();
                    this.uoE = x;
                    break;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetTouchState();
                break;
            case MotionEvent.ACTION_MOVE:
                if (this.mActivePointerId != -1) {
                    determineScrollingStart(motionEvent, 1.0f);
                    break;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(motionEvent);
                releaseVelocityTracker();
                break;
        }
        return this.mTouchState != 0;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        if (this.uoA == null) {
            return super.onTouchEvent(motionEvent);
        }
        acquireVelocityTrackerAndAddMovement(motionEvent);
        float x;
        float y;
        int abs;
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                boolean z;
                x = motionEvent.getX();
                y = motionEvent.getY();
                this.mDownX = x;
                this.mDownY = y;
                this.uoF = (float) this.panelX;
                this.mLastMotionX = x;
                this.mTotalMotionX = 0.0f;
                this.mActivePointerId = motionEvent.getPointerId(0);
                abs = Math.abs(this.slidingPanelLayoutInterpolator.mFinalX - this.panelX);
                z = this.slidingPanelLayoutInterpolator.isFinished() || abs < this.mTouchSlop / 3;
                if (z && !this.mForceDrag) {
                    return true;
                }
                this.mForceDrag = false;
                cnN();
                this.uoE = x;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (this.mTouchState != 1) {
                    return true;
                }
                this.mVelocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                abs = (int) this.mVelocityTracker.getXVelocity(this.mActivePointerId);
                boolean z2 = this.mTotalMotionX > 25.0f && Math.abs(abs) > this.mFlingThresholdVelocity;
                if (z2) {
                    if (this.mIsRtl) {
                        abs = -abs;
                    }
                    if (Math.abs(abs) < this.mMinFlingVelocity) {
                        if (abs >= 0) {
                            fv(750);
                        } else {//Todo: this else was not there initially
                            closePanel(750);
                        }
                    } else {
                        float measuredWidth = ((float) (getMeasuredWidth() / 2)) + (((float) Math.sin((double) ((float) (((double) (Math.min(1.0f, (((float) (abs < 0 ? this.panelX : getMeasuredWidth() - this.panelX)) * 1.0f) / ((float) getMeasuredWidth())) - 0.5f)) * 0.4712389167638204d)))) * ((float) (getMeasuredWidth() / 2)));
                        z2 = abs > 0;
                        abs = Math.round(Math.abs(measuredWidth / ((float) Math.max(this.mMinSnapVelocity, Math.abs(abs)))) * 1000.0f) * 4;
                        if (z2) {
                            fv(abs);
                        } else {
                            closePanel(abs);
                        }
                    }
                } else {
                    if (this.panelX >= getMeasuredWidth() / 2) {
                        fv(750);
                    } else {//Todo: this else was not there initially
                        closePanel(750);
                    }
                }
                resetTouchState();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (this.mTouchState == 1) {
                    abs = motionEvent.findPointerIndex(this.mActivePointerId);
                    if (abs == -1) {
                        return true;
                    }
                    y = motionEvent.getX(abs);
                    this.mTotalMotionX += Math.abs(y - this.mLastMotionX);
                    this.mLastMotionX = y;
                    y -= this.uoE;
                    x = this.uoF;
                    if (this.mIsRtl) {
                        y = -y;
                    }
                    BM((int) (y + x));
                    return true;
                }
                determineScrollingStart(motionEvent, 1.0f);
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(motionEvent);
                releaseVelocityTracker();
                return true;
            default:
                return true;
        }
    }

    private void resetTouchState() {
        releaseVelocityTracker();
        this.mForceDrag = false;
        this.mTouchState = 0;
        this.mActivePointerId = -1;
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() >> 8) & 255;
        if (motionEvent.getPointerId(action) == this.mActivePointerId) {
            action = action == 0 ? 1 : 0;
            float x = motionEvent.getX(action);
            this.uoE += x - this.mLastMotionX;
            this.mDownX = x;
            this.mLastMotionX = x;
            this.mActivePointerId = motionEvent.getPointerId(action);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    void determineScrollingStart(MotionEvent motionEvent, float f) {
        int findPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
        if (findPointerIndex != -1) {
            float x = motionEvent.getX(findPointerIndex);
            if (((int) Math.abs(x - this.mDownX)) > Math.round(((float) this.mTouchSlop) * f)) {
                this.mTotalMotionX += Math.abs(this.mLastMotionX - x);
                this.uoE = x;
                this.mLastMotionX = x;
                cnN();
            }
        }
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent motionEvent) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
            this.mVelocityTracker.clear();
        }
        this.mVelocityTracker.addMovement(motionEvent);
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.clear();
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    protected void onMeasure(int i, int i2) {
        int size = MeasureSpec.getSize(i);
        int size2 = MeasureSpec.getSize(i2);
        if (this.uoB != null) {
            this.uoB.measure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(size2, MeasureSpec.EXACTLY));//Todo: i modified them, there was ints before instead of constants
        }
        if (this.uoA != null) {
            this.uoA.measure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(size2, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(size, size2);
        BM((int) (((float) size) * this.mPanelPositionRatio));
    }

    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.uoB != null) {
            this.uoB.layout(0, 0, this.uoB.getMeasuredWidth(), this.uoB.getMeasuredHeight());
        }
        if (this.uoA != null) {
            int measuredWidth = this.uoA.getMeasuredWidth();
            int measuredHeight = this.uoA.getMeasuredHeight();
            int i5 = this.mIsRtl ? measuredWidth : -measuredWidth;
            if (this.mIsRtl) {
                measuredWidth *= 2;
            } else {
                measuredWidth = 0;
            }
            this.uoA.layout(i5, 0, measuredWidth, measuredHeight);
        }
    }

    public static boolean isRtl(Resources resources) {
        return resources.getConfiguration().getLayoutDirection() == 1;
    }

    @SuppressLint("WrongConstant")
    private void cnN() {
        this.mTouchState = 1;
        this.mIsPageMoving = true;
        this.mSettling = false;
        this.slidingPanelLayoutInterpolator.cnP();
        if (uoL) {
            setLayerType(2, null);
        }
        if (this.dragCallback != null) {
            this.dragCallback.drag();
        }
    }

    public final void cnF() {
        this.mIsPageMoving = true;
        if (this.dragCallback != null) {
            this.dragCallback.onPanelOpening();
        }
    }

    public final void cnG() {
        cnO();
        this.mIsPanelOpen = true;
        this.mIsPageMoving = false;
        if (this.dragCallback != null) {
            this.dragCallback.onPanelOpen();
        }
    }

    @SuppressLint("WrongConstant")
    final void cnO() {
        if (uoL) {
            setLayerType(0, null);
        }
    }
}