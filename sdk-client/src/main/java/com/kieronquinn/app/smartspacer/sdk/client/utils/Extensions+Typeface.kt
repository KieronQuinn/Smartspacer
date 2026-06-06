package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.graphics.Typeface

val expressiveTitleTypeface by lazy {
    Typeface.create(
        "variable-title-medium-emphasized", Typeface.NORMAL
    ).takeUnless { it == Typeface.DEFAULT }
}

val expressiveSubtitleTypeface by lazy {
    Typeface.create(
        "variable-title-medium", Typeface.NORMAL
    ).takeUnless { it == Typeface.DEFAULT }
}