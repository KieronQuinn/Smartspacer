package com.kieronquinn.app.smartspacer.sdk.client.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ViewFlipper

class SafeViewFlipper @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ViewFlipper(context, attrs) {

    override fun onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow()
        } catch (e: IllegalArgumentException) {
            stopFlipping()
        }
    }

}