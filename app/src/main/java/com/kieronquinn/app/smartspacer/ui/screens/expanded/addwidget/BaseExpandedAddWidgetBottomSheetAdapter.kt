package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.SizeF
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.RequestManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.glide.Widget
import com.kieronquinn.app.smartspacer.ui.views.appwidget.PreviewAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.extensions.getBestRemoteViews
import com.kieronquinn.app.smartspacer.utils.extensions.loadPreview

interface BaseExpandedAddWidgetBottomSheetAdapter {

    fun setupWidget(
        widgetContext: Context,
        glide: RequestManager,
        info: AppWidgetProviderInfo,
        spanX: Int,
        spanY: Int,
        columnWidth: Int,
        rowHeight: Int,
        label: CharSequence,
        description: CharSequence?,
        root: ViewGroup,
        imageImageView: ImageView,
        containerLinearLayout: LinearLayout,
        nameTextView: TextView,
        descriptionTextView: TextView
    ) {
        val context = root.context
        val spanWidth = columnWidth * spanX
        val spanHeight = rowHeight * spanY
        val previewView = info.loadPreview()?.getBestRemoteViews(
            context,
            SizeF(spanWidth.toFloat(), spanHeight.toFloat())
        )
        if (previewView != null) {
            imageImageView.isVisible = false
            containerLinearLayout.isVisible = true
            containerLinearLayout.updateLayoutParams<LinearLayout.LayoutParams> {
                height = spanHeight
            }
            val appWidgetHostView = PreviewAppWidgetHostView(widgetContext)
            appWidgetHostView.setAppWidget(
                info,
                previewView,
                spanWidth,
                spanHeight
            )
            appWidgetHostView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                spanHeight
            )
            containerLinearLayout.removeAllViews()
            containerLinearLayout.addView(appWidgetHostView)
        } else {
            imageImageView.isVisible = true
            containerLinearLayout.isVisible = false
            imageImageView.updateLayoutParams<LinearLayout.LayoutParams> {
                width = spanWidth
                height = spanHeight
            }
            glide.load(Widget(info, spanWidth, spanHeight))
                .into(imageImageView)
                .waitForLayout()
        }
        nameTextView.text = root.context.getString(
            R.string.expanded_add_widget_widget_label,
            label,
            spanX,
            spanY
        )
        descriptionTextView.text = description
        descriptionTextView.isVisible = description != null
        root.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
    }

}