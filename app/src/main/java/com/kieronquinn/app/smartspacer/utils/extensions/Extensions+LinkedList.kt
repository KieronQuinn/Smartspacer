package com.kieronquinn.app.smartspacer.utils.extensions

import java.util.*

fun <T> LinkedList<T>.popOrNull(req: (() -> Boolean) = { true }): T? {
    return if(req() && size > 0) pop() else null
}