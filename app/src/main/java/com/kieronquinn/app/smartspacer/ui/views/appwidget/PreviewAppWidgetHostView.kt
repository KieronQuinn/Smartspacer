package com.kieronquinn.app.smartspacer.ui.views.appwidget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.ui.views.RoundedCornersEnforcingAppWidgetHostView
import kotlin.math.max
import kotlin.math.min

class PreviewAppWidgetHostView(context: Context): RoundedCornersEnforcingAppWidgetHostView(context) {

    companion object {
        /**
         * The maximum dimension that can be used as the size in
         * [android.view.View.MeasureSpec.makeMeasureSpec].
         *
         *
         * This is equal to (1 << MeasureSpec.MODE_SHIFT) - 1.
         */
        private const val MAX_MEASURE_SPEC_DIMENSION = (1 shl 30) - 1
    }

    private var mTargetPreviewWidth: Int = 0
    private var mTargetPreviewHeight: Int = 0
    private var mPreviewContainerScale = 1f
    private var forceLayoutWidth: Int? = null
    private var forceLayoutHeight: Int? = null

    init {
        clipToOutline = false
        clipChildren = false
        clipToPadding = false
    }

    fun setAppWidget(
        info: AppWidgetProviderInfo,
        remoteViews: RemoteViews,
        width: Int,
        height: Int
    ) {
        enforceRoundedCorners = false
        mTargetPreviewWidth = width
        mTargetPreviewHeight = height
        setAppWidget(-1, info)
        updateAppWidget(remoteViews)
        val scale = measureAndComputeWidgetPreviewScale()
        scaleX = scale
        scaleY = scale
        enforceRoundedCorners = true
    }

    //Override sizing of layout if required to prevent scale from impacting the sizing too
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val forceWidth = forceLayoutWidth
        val forceHeight = forceLayoutHeight
        if(forceWidth != null && forceHeight != null) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(forceWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(forceHeight, MeasureSpec.EXACTLY)
            )
        }else{
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private fun measureAndComputeWidgetPreviewScale(): Float {
        if (childCount != 1) {
            return 1f
        }

        // Measure the largest possible width & height that the app widget wants to display.
        measure(
            MeasureSpec.makeMeasureSpec(
                MAX_MEASURE_SPEC_DIMENSION,
                MeasureSpec.UNSPECIFIED
            ),
            MeasureSpec.makeMeasureSpec(
                MAX_MEASURE_SPEC_DIMENSION,
                MeasureSpec.UNSPECIFIED
            )
        )
        // If RemoteViews contains multiple sizes, the best fit sized RemoteViews will be
        // selected in onLayout. To work out the right measurement, let's layout and then
        // measure again.
        layout( /* left= */
            0,  /* top= */
            0,  /* right= */
            mTargetPreviewWidth,  /* bottom= */
            mTargetPreviewHeight
        )
        measure(
            MeasureSpec.makeMeasureSpec(mTargetPreviewWidth, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(mTargetPreviewHeight, MeasureSpec.UNSPECIFIED)
        )
        val widgetContent: View = getChildAt(0)
        val appWidgetContentWidth = max(widgetContent.measuredWidth, mTargetPreviewWidth)
        val appWidgetContentHeight = max(widgetContent.measuredHeight, mTargetPreviewHeight)
        if (appWidgetContentWidth == 0 || appWidgetContentHeight == 0) {
            return 1f
        }
        layout( /* left= */
            0,  /* top= */
            0,  /* right= */
            appWidgetContentWidth,  /* bottom= */
            appWidgetContentHeight
        )

        // If the width / height of the widget content is set to wrap content, overrides the width /
        // height with the measured dimension. This avoids incorrect measurement after scaling.
        val layoutParam = widgetContent.layoutParams as LayoutParams
        if (layoutParam.width == ViewGroup.LayoutParams.WRAP_CONTENT ||
            layoutParam.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            layoutParam.width = appWidgetContentWidth
        }

        widgetContent.setLayoutParams(layoutParam)
        val horizontalPadding: Int = paddingStart + paddingEnd
        val verticalPadding: Int = paddingTop + paddingBottom

        forceLayoutWidth = appWidgetContentWidth
        forceLayoutHeight = appWidgetContentHeight
        return min(
            ((mTargetPreviewWidth - horizontalPadding) * mPreviewContainerScale / appWidgetContentWidth),
            ((mTargetPreviewHeight - verticalPadding) * mPreviewContainerScale / appWidgetContentHeight)
        )
    }

}