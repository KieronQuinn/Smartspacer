package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.model.widget.RemoteWidgetView.Companion.KEY_IDENTIFIER
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListNullableCompat

data class TextView(
    override val identifier: String?,
    val text: CharSequence?,
    val textSize: Float,
    val textColor: Int,
    val compoundDrawables: ArrayList<Icon?>
): RemoteWidgetView<TextView> {

    companion object {
        private const val KEY_TEXT = "text"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_COMPOUND_DRAWABLES = "compound_drawables"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getString(KEY_IDENTIFIER),
        bundle.getCharSequence(KEY_TEXT),
        bundle.getFloat(KEY_TEXT_SIZE),
        bundle.getInt(KEY_TEXT_COLOR),
        bundle.getParcelableArrayListNullableCompat(KEY_COMPOUND_DRAWABLES, Icon::class.java)!!
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun toBundle(bundle: Bundle): Bundle {
        super.toBundle(bundle)
        bundle.putCharSequence(KEY_TEXT, text)
        bundle.putFloat(KEY_TEXT_SIZE, textSize)
        bundle.putInt(KEY_TEXT_COLOR, textColor)
        bundle.putParcelableArrayList(KEY_COMPOUND_DRAWABLES, compoundDrawables)
        return bundle
    }

}