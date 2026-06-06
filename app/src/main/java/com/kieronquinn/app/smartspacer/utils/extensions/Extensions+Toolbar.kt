package com.kieronquinn.app.smartspacer.utils.extensions

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun Toolbar.onNavigationIconClicked() = callbackFlow<View> {
    setNavigationOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)

fun CollapsingToolbarLayout.animateScrimColourTo(colour: Int) {
    (tag as? ValueAnimator)?.cancel()
    val current = (contentScrim as? ColorDrawable)?.color ?: Color.TRANSPARENT
    if (current == colour) return
    val start = if (current == Color.TRANSPARENT) colour and 0x00FFFFFF else current
    val end = if (colour == Color.TRANSPARENT) current and 0x00FFFFFF else colour
    tag = ValueAnimator.ofArgb(start, end).apply {
        addUpdateListener {
            setContentScrimColor(it.animatedValue as Int)
        }
        start()
    }
}