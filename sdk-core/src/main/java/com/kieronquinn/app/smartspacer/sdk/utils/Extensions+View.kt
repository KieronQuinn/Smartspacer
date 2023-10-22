package com.kieronquinn.app.smartspacer.sdk.utils

import android.app.PendingIntent
import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children


/**
 *  Equivalent of [View.findViewById], but takes an identifier in the format
 *  `$PACKAGE_NAME:id/$VIEW_ID`, where `VIEW_ID` is a string such as "content"
 */
fun <T : View?> View.findViewByIdentifier(identifier: String, context: Context = getContext()): T? {
    val id = context.getResourceForIdentifier(identifier) ?: return null
    return findViewById<T>(id)
}

/**
 *  Returns the first View of a given type [T] which are a child of the [View] recursively, or null.
 *  Breadth first search.
 */
inline fun <reified T: View?> View.findByType(): T? {
    return nthOfType<T>(0)
}

/**
 *  Returns the nth view of a given type [T] which are a child of the [View] recursively, or null.
 *  Breadth first search.
 */
inline fun <reified T: View?> View.nthOfType(n: Int): T? {
    return allChildren().filterIsInstance<T>().getOrNull(n)
}

inline fun <reified T: View?> View.nthChild(n: Int): T? {
    if(this !is ViewGroup) return null
    return children.toList().getOrNull(n) as? T
}

/**
 *  Returns a flat list of all [View]s that are child of a given [View], recursively. Breadth first
 *  search.
 *
 *  Based on https://stackoverflow.com/a/18669307/1088334
 */
fun View.allChildren(): List<View> {
    val visited = ArrayList<View>()
    val unvisited = ArrayList<View>()
    unvisited.add(this)
    while (unvisited.isNotEmpty()) {
        val child = unvisited.removeAt(0)
        visited.add(child)
        if (child !is ViewGroup) continue
        val childCount = child.childCount
        for (i in 0 until childCount) {
            unvisited.add(child.getChildAt(i))
        }
    }
    return visited
}

/**
 *  Prints the current View tree to logcat, with a given [tag]. Each layer is indented by a space.
 */
fun View.dumpToLog(tag: String, indent: Int = 0, index: Int = 0) {
    Log.d(tag, "${" ".repeat(indent)}${this::class.java.simpleName}[${getDumpData(index)}]")
    if(this is ViewGroup){
        children.forEachIndexed { index, view ->
            view.dumpToLog(tag, indent + 1, index)
        }
    }
}

private fun View.getDumpData(index: Int): String {
    val extra = when(this) {
        is TextView -> ", text=$text"
        is ImageView -> ", drawable=$drawable, contentDescription=$contentDescription"
        else -> ""
    }
    return "index=$index id=${getResourceName()} ($id), pendingIntent=${getClickPendingIntent()}, clickable=$isClickable $extra"
}

fun View.getClickPendingIntent(): PendingIntent? {
    val pendingIntentTag = resources.getIdentifier(
        "pending_intent_tag", "id", "android"
    )
    return getTag(pendingIntentTag) as? PendingIntent ?: tag as? PendingIntent
}

fun View.getResourceName(): String? {
    if(id == 0) return null
    return context.resources.getResourceNameOrNull(id)
}

private fun Resources.getResourceNameOrNull(resource: Int, checkSystem: Boolean = true): String? {
    return try {
        getResourceName(resource)
    }catch (e: Resources.NotFoundException){
        if(checkSystem) {
            Resources.getSystem().getResourceNameOrNull(resource, false)
        }else null
    }
}