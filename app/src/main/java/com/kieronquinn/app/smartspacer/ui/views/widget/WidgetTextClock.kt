package com.kieronquinn.app.smartspacer.ui.views.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextClock
import com.kieronquinn.app.smartspacer.utils.extensions.convertToGoogleSans

class WidgetTextClock constructor(
    context: Context, attributeSet: AttributeSet?
): TextClock(context, attributeSet) {

    override fun setTypeface(tf: Typeface?, style: Int) {
        super.setTypeface(tf?.convertToGoogleSans(), style)
    }

    override fun setTypeface(tf: Typeface?) {
        super.setTypeface(tf?.convertToGoogleSans())
    }

}