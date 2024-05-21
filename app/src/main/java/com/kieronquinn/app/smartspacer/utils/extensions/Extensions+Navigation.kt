package com.kieronquinn.app.smartspacer.utils.extensions

import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.ui.NavigationUI
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun NavController.onDestinationChanged() = callbackFlow {
    val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
        trySend(destination)
    }
    addOnDestinationChangedListener(listener)
    awaitClose {
        removeOnDestinationChangedListener(listener)
    }
}.debounce(TAP_DEBOUNCE)

fun NavController.setOnBackPressedCallback(callback: OnBackPressedCallback) {
    NavController::class.java.getDeclaredField("onBackPressedCallback").apply {
        isAccessible = true
    }.set(this, callback)
}

/**
 *  Copy of [NavigationUI.onNavDestinationSelected] but without the force unwrap on the animation
 *  which breaks if the stack is cleared before it is used.
 */
fun NavController.onNavDestinationSelected(item: MenuItem): Boolean {
    val builder = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
    if (item.order and Menu.CATEGORY_SECONDARY == 0) {
        builder.setPopUpTo(
            graph.findStartDestination().id,
            inclusive = false,
            saveState = true
        )
    }
    val options = builder.build()
    return try {
        // TODO provide proper API instead of using Exceptions as Control-Flow.
        navigate(item.itemId, null, options)
        // Return true only if the destination we've navigated to matches the MenuItem
        currentDestination?.matchDestination(item.itemId) == true
    } catch (e: IllegalArgumentException) {
        val name = NavDestination.getDisplayName(context, item.itemId)
        Log.i(
            "NavigationUI",
            "Ignoring onNavDestinationSelected for MenuItem $name as it cannot be found " +
                    "from the current destination $currentDestination",
            e
        )
        false
    }
}

internal fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { it.id == destId }