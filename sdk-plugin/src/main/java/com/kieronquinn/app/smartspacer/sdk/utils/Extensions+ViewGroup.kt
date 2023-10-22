package com.kieronquinn.app.smartspacer.sdk.utils

import android.view.View
import android.view.ViewGroup

/**
 *  Returns a list of all Views of type [clazz] which are in this [ViewGroup] tree. This can be
 *  useful if you want the nth TextView or ImageView from a widget using Jetpack Glance, and whose
 *  structure changes too much for the ViewStructure API to be viable.
 */
fun <T : View> ViewGroup.findViewsByType(clazz: Class<T>): List<T> {
    return mutableListOf<T?>().apply {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            (child as? ViewGroup)?.let {
                addAll(child.findViewsByType(clazz))
            }
            if (clazz.isInstance(child))
                add(clazz.cast(child))
        }
    }.filterNotNull()
}