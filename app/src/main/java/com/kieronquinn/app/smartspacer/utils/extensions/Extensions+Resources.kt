package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.res.Resources
import androidx.annotation.ArrayRes

fun Resources.dip(value: Int): Int = (value * displayMetrics.density).toInt()

val Int.dp
    get() = Resources.getSystem().dip(this)

fun Resources.px(value: Int): Int = (value / displayMetrics.density).toInt()

val Int.px
    get() = Resources.getSystem().px(this)

fun Resources.getResourceIdArray(@ArrayRes resourceId: Int): Array<Int> {
    val array = obtainTypedArray(resourceId)
    val items = mutableListOf<Int>()
    for(i in 0 until array.length()){
        items.add(array.getResourceId(i, 0))
    }
    array.recycle()
    return items.toTypedArray()
}

fun Resources.getResourceNameOrNull(resource: Int, checkSystem: Boolean = true): String? {
    return try {
        getResourceName(resource)
    }catch (e: Resources.NotFoundException){
        if(checkSystem) {
            Resources.getSystem().getResourceNameOrNull(resource, false)
        }else null
    }
}