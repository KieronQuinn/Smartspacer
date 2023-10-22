package com.kieronquinn.app.smartspacer.utils.extensions

import android.text.TextUtils

/**
 *  Trims a given [CharSequence] to [length], appending an ellipsis (and removing 1 char to cope)
 *  if needed.
 */
fun CharSequence.takeEllipsised(length: Int): CharSequence {
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

/**
 *  Either [remove]s the bullet from the prefix of a given [CharSequence] or otherwise moves it to
 *  the end.
 */
fun CharSequence.reformatBullet(remove: Boolean): CharSequence {
    val prefix = "· "
    val suffix = " ·"
    return when {
        remove -> {
            removePrefix(prefix)
        }
        startsWith(prefix) -> {
            TextUtils.concat(removePrefix(prefix), suffix)
        }
        else -> this
    }
}