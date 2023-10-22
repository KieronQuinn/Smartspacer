package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.app.WallpaperColors
import android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

fun WallpaperManager.lockscreenWallpaperSupportsDarkText(scope: CoroutineScope) = callbackFlow {
    val listener = WallpaperManager.OnColorsChangedListener { _, which ->
        if(which and WallpaperManager.FLAG_LOCK != 0) {
            trySend(lockscreenSupportsDarkText())
        }
    }
    addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
    awaitClose {
        removeOnColorsChangedListener(listener)
    }
}.stateIn(scope, SharingStarted.Eagerly, lockscreenSupportsDarkText())

fun WallpaperManager.homescreenWallpaperSupportsDarkText(scope: CoroutineScope) = callbackFlow {
    val listener = WallpaperManager.OnColorsChangedListener { _, which ->
        if(which and WallpaperManager.FLAG_SYSTEM != 0) {
            trySend(homescreenSupportsDarkText())
        }
    }
    addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
    awaitClose {
        removeOnColorsChangedListener(listener)
    }
}.stateIn(scope, SharingStarted.Eagerly, homescreenSupportsDarkText())

@SuppressLint("InlinedApi")
private fun WallpaperManager.lockscreenSupportsDarkText(): Boolean {
    val colors = getWallpaperColors(WallpaperManager.FLAG_LOCK)
        ?: getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return false
    return (colors.getColorHintsCompat() and HINT_SUPPORTS_DARK_TEXT) != 0
}

@SuppressLint("InlinedApi")
private fun WallpaperManager.homescreenSupportsDarkText(): Boolean {
    val colors = getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return false
    return (colors.getColorHintsCompat() and HINT_SUPPORTS_DARK_TEXT) != 0
}

/**
 *  [WallpaperColors.getColorHints] exists on older versions but is hidden
 */
@SuppressLint("NewApi") //Exists, but is hidden
private fun WallpaperColors.getColorHintsCompat(): Int {
    return colorHints
}
