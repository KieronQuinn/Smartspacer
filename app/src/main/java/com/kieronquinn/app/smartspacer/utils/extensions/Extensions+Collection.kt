package com.kieronquinn.app.smartspacer.utils.extensions

fun Collection<Int>.or(): Int {
    var current = 0
    forEach {
        current = current.or(it)
    }
    return current
}