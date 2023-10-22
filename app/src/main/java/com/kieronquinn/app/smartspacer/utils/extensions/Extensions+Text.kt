package com.kieronquinn.app.smartspacer.utils.extensions

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.core.text.getSpans
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.utils.spans.SmartspacerForegroundColorSpan
import android.app.smartspace.uitemplatedata.Text as SystemText


fun Text.toSystemText(): SystemText {
    return SystemText.Builder(text)
        .setMaxLines(maxLines)
        .setTruncateAtType(truncateAtType)
        .build()
}

fun SystemText.toText(): Text {
    return Text(
        text = text,
        maxLines = maxLines,
        truncateAtType = truncateAtType
    )
}

fun Text.cloneWithTextColour(colour: Int): Text {
    return copy(text = SpannableStringBuilder(text).apply {
        //Remove any existing instances of our special foreground span so we don't add lots of spans
        getSpans<SmartspacerForegroundColorSpan>().forEach {
            removeSpan(it)
        }
        //The base text colour has a priority of minimum value so any customisation overrides it
        setSpan(
            SmartspacerForegroundColorSpan(colour),
            0,
            length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE or getSpanPriorityFlags(-Integer.MAX_VALUE)
        )
    })
}

fun getSpanPriorityFlags(priority: Int): Int {
    return priority shl Spanned.SPAN_PRIORITY_SHIFT
}