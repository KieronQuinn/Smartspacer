/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kieronquinn.app.smartspacer.utils.appwidget

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import com.kieronquinn.app.smartspacer.R

/**
 * Utilities to compute the enforced the use of rounded corners on App Widgets.
 *
 * Based on https://android.googlesource.com/platform/packages/apps/Launcher3/+/master/src/com/android/launcher3/widget/RoundedCornerEnforcement.java
 */
object RoundedCornerEnforcement {

    /**
     * Find the background view for a widget.
     *
     * @param appWidget the view containing the App Widget (typically the instance of
     * [AppWidgetHostView]).
     */
    fun findBackground(appWidget: View): View? {
        val backgrounds = findViewsWithId(appWidget, android.R.id.background)
        if (backgrounds.size == 1) {
            return backgrounds[0]
        }
        // Really, the argument should contain the widget, so it cannot be the background.
        if (appWidget is ViewGroup) {
            val vg = appWidget
            if (vg.childCount > 0) {
                return findUndefinedBackground(vg.getChildAt(0))
            }
        }
        return appWidget
    }

    /**
     * Check whether the app widget has opted out of the enforcement.
     */
    fun hasAppWidgetOptedOut(appWidget: View, background: View): Boolean {
        return background.id == android.R.id.background && background.clipToOutline
    }

    /**
     * Computes the rounded rectangle needed for this app widget.
     *
     * @param appWidget View onto which the rounded rectangle will be applied.
     * @param background Background view. This must be either `appWidget` or a descendant
     * of `appWidget`.
     * @param outRect Rectangle set to the rounded rectangle coordinates, in the reference frame
     * of `appWidget`.
     */
    fun computeRoundedRectangle(
        appWidget: View, background: View,
        outRect: Rect
    ) {
        var background = background
        outRect.left = 0
        outRect.right = background.width
        outRect.top = 0
        outRect.bottom = background.height
        while (background !== appWidget) {
            outRect.offset(background.left, background.top)
            background = background.parent as View
        }
    }

    /**
     * Computes the radius of the rounded rectangle that should be applied to a widget expanded
     * in the given context.
     */
    fun computeEnforcedRadius(context: Context): Float {
        val res = context.resources
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return res.getDimension(R.dimen.enforced_rounded_corner_max_radius)
        }
        val systemRadius = res.getDimension(android.R.dimen.system_app_widget_background_radius)
        val defaultRadius =
            res.getDimension(R.dimen.enforced_rounded_corner_max_radius)
        return defaultRadius.coerceAtMost(systemRadius)
    }

    private fun findViewsWithId(view: View, @IdRes viewId: Int): List<View> {
        val output: MutableList<View> = ArrayList()
        accumulateViewsWithId(view, viewId, output)
        return output
    }

    // Traverse views. If the predicate returns true, continue on the children, otherwise, don't.
    private fun accumulateViewsWithId(view: View, @IdRes viewId: Int, output: MutableList<View>) {
        if (view.id == viewId) {
            output.add(view)
            return
        }
        if (view is ViewGroup) {
            val vg = view
            for (i in 0 until vg.childCount) {
                accumulateViewsWithId(vg.getChildAt(i), viewId, output)
            }
        }
    }

    private fun isViewVisible(view: View): Boolean {
        return if (view.visibility != View.VISIBLE) {
            false
        } else !view.willNotDraw() || view.foreground != null || view.background != null
    }

    private fun findUndefinedBackground(current: View): View? {
        if (current.visibility != View.VISIBLE) {
            return null
        }
        if (isViewVisible(current)) {
            return current
        }
        var lastVisibleView: View? = null
        // Find the first view that is either not a ViewGroup, or a ViewGroup which will draw
        // something, or a ViewGroup that contains more than one view.
        if (current is ViewGroup) {
            val vg = current
            for (i in 0 until vg.childCount) {
                val visibleView = findUndefinedBackground(vg.getChildAt(i))
                if (visibleView != null) {
                    if (lastVisibleView != null) {
                        return current // At least two visible children
                    }
                    lastVisibleView = visibleView
                }
            }
        }
        return lastVisibleView
    }
}