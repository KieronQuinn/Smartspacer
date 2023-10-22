package com.kieronquinn.app.smartspacer.sdk.client.utils

import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun ConstraintLayout.setAspectRatio(
    id: Int,
    aspect: String? = null,
    aspectWidth: Int? = null,
    aspectHeight: Int? = null
) {
    val aspectRatio = aspect ?: "$aspectWidth:$aspectHeight"
    val set = ConstraintSet().apply {
        clone(this@setAspectRatio)
    }
    set.setDimensionRatio(id, aspectRatio)
    set.applyTo(this)
}