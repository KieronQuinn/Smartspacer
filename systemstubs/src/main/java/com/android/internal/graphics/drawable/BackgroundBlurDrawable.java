package com.android.internal.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

public class BackgroundBlurDrawable extends Drawable {

    @Override
    public void draw(@NonNull Canvas canvas) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int getOpacity() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setAlpha(int alpha) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Color that will be alpha blended on top of the blur.
     */
    public void setColor(@ColorInt int color) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Blur radius in pixels.
     */
    public void setBlurRadius(int blurRadius) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the corner radius, in degrees.
     */
    public void setCornerRadius(float cornerRadius) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the corner radius in degrees.
     * @param cornerRadiusTL top left radius.
     * @param cornerRadiusTR top right radius.
     * @param cornerRadiusBL bottom left radius.
     * @param cornerRadiusBR bottom right radius.
     */
    public void setCornerRadius(float cornerRadiusTL, float cornerRadiusTR, float cornerRadiusBL,
                                float cornerRadiusBR) {
        throw new RuntimeException("Stub!");
    }

    public static final class Aggregator {
        /**
         * Called when a BackgroundBlurDrawable has been updated
         */
        @UiThread
        void onBlurDrawableUpdated(BackgroundBlurDrawable drawable) {
            throw new RuntimeException("Stub!");
        }
    }

}
