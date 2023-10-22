package com.kieronquinn.app.smartspacer.utils.extensions

fun Int.orNullIfZero(): Int? {
    return if(this == 0) null else this
}