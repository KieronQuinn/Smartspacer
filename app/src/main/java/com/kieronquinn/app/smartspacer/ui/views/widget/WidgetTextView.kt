package com.kieronquinn.app.smartspacer.ui.views.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import com.kieronquinn.app.smartspacer.utils.extensions.convertToGoogleSans

@SuppressLint("AppCompatCustomView")
class WidgetTextView constructor(
    context: Context, attributeSet: AttributeSet?
): TextView(context, attributeSet) {

    override fun setTypeface(tf: Typeface?, style: Int) {
        super.setTypeface(tf?.convertToGoogleSans(), style)
    }

    override fun setTypeface(tf: Typeface?) {
        super.setTypeface(tf?.convertToGoogleSans())
    }

}