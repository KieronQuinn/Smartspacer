package com.kieronquinn.app.smartspacer.ui.views.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.Chronometer
import com.kieronquinn.app.smartspacer.utils.extensions.convertToGoogleSans

class WidgetChronometer constructor(
    context: Context, attributeSet: AttributeSet?
): Chronometer(context, attributeSet) {

    override fun setTypeface(tf: Typeface?, style: Int) {
        super.setTypeface(tf?.convertToGoogleSans(), style)
    }

    override fun setTypeface(tf: Typeface?) {
        super.setTypeface(tf?.convertToGoogleSans())
    }

}