package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.widget.TextView
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun TextView.setText(text: Text, textColour: Int) {
    this.text = text.text
    this.ellipsize = text.truncateAtType
    this.maxLines = text.maxLines
    this.setTextColor(textColour)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun TextView.setText(text: CharSequence?, textColour: Int) {
    this.text = text
    this.setTextColor(textColour)
}