package com.kieronquinn.app.smartspacer.sdk.utils

/**
 *  Trims a given [CharSequence] to [length], appending an ellipsis (and removing 1 char to cope)
 *  if needed.
 */
internal fun CharSequence.takeEllipsised(length: Int): CharSequence {
    return when {
        length == 0 -> {
            ""
        }
        length >= this.length -> {
            this
        }
        else -> {
            "${this.take(length - 1)}…"
        }
    }
}