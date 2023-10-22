package com.kieronquinn.app.smartspacer.utils.extensions

/**
 *  Trims a given [String] to [length], appending an ellipsis (and removing 1 char to cope) if
 *  needed.
 */
fun String.takeEllipsised(length: Int): CharSequence {
    return when {
        length == 0 -> {
            ""
        }
        length >= this.length -> {
            this
        }
        else -> {
            "${this.take(length - 1).trim()}…"
        }
    }
}