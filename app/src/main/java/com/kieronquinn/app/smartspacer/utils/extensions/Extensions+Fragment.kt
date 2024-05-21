package com.kieronquinn.app.smartspacer.utils.extensions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun Fragment.childBackStackTopFragment() = callbackFlow {
    val listener = FragmentManager.OnBackStackChangedListener {
        trySend(getTopFragment())
    }
    childFragmentManager.addOnBackStackChangedListener(listener)
    trySend(getTopFragment())
    awaitClose {
        childFragmentManager.removeOnBackStackChangedListener(listener)
    }
}

fun Fragment.getTopFragment(): Fragment? {
    if(!isAdded) return null
    return childFragmentManager.fragments.firstOrNull()
}

fun Fragment.getBackFragment(): Fragment? {
    if(!isAdded) return null
    return childFragmentManager.fragments.getOrNull(1)
}

/**
 *  Helper for [LifecycleOwner].[whenResumed]
 */
fun Fragment.whenResumed(block: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.whenResumed(block)
}

/**
 *  Helper for [LifecycleOwner].[whenCreated]
 */
fun Fragment.whenCreated(block: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.whenCreated(block)
}