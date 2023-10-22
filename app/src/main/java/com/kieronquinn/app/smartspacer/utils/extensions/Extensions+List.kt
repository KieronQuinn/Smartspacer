package com.kieronquinn.app.smartspacer.utils.extensions

fun List<*>.deepEquals(other : List<*>, eq: Any?.(Any?) -> Boolean = { equals(it) }) =
    this.size == other.size && this.mapIndexed { i, e -> eq(e, other[i]) }.all { it }

/**
 *  Splits a given list using the [predicate], returning a pair of the original list with items
 *  removed and the list of removed items
 */
fun <T> List<T>.split(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val splitList = ArrayList<T>()
    val originalList = filterNot {
        if(predicate(it)) {
            splitList.add(it)
            true
        }else false
    }
    return Pair(originalList, splitList)
}