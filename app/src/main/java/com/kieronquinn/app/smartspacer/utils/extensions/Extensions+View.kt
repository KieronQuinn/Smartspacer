package com.kieronquinn.app.smartspacer.utils.extensions

import android.graphics.Outline
import android.view.View
import android.view.ViewGroup
import android.view.ViewHidden
import android.view.ViewOutlineProvider
import android.view.ViewRootImpl
import android.view.Window
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val TAP_DEBOUNCE = 250L

suspend fun View.awaitPost() = suspendCancellableCoroutine {
    post {
        if(isAttachedToWindow){
            it.resume(this)
        }else{
            it.cancel()
        }
    }
}

fun View.getViewRootImpl(): ViewRootImpl? {
    return Refine.unsafeCast<ViewHidden>(this).viewRootImpl
}

fun View.onClicked() = callbackFlow {
    setOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)

fun View.onLongClicked(vibrate: Boolean = true) = callbackFlow<View> {
    setOnLongClickListener {
        trySend(it)
        vibrate
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)

/**
 *  The standard [Window.setNavigationBarColor] and [Window.setStatusBarColor] don't work for
 *  this embedded window so we make them invisible manually
 */
fun View.removeStatusNavBackgroundOnPreDraw() = apply {
    doOnPreDraw {
        val statusBarBackground = it.findViewById<View>(android.R.id.statusBarBackground)
        statusBarBackground?.run {
            visibility = View.INVISIBLE
            alpha = 0f
        }
        val navigationBarBackground = it.findViewById<View>(android.R.id.navigationBarBackground)
        navigationBarBackground?.run {
            visibility = View.INVISIBLE
            alpha = 0f
        }
    }
}

fun View.setRecursiveLongClickListener(listener: View.OnLongClickListener?) {
    //Setting just a long click listener breaks click listeners
    if(isClickable) {
        isLongClickable = listener != null
        setOnLongClickListener(listener)
    }
    if(this is ViewGroup){
        children.forEach { it.setRecursiveLongClickListener(listener) }
    }
}

fun View.isAttached() = callbackFlow {
    val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            trySend(true)
        }

        override fun onViewDetachedFromWindow(v: View) {
            trySend(false)
        }
    }
    addOnAttachStateChangeListener(listener)
    if (isAttachedToWindow) {
        trySend(true)
    }
    awaitClose {
        removeOnAttachStateChangeListener(listener)
    }
}.distinctUntilChanged()

fun View.setRoundedOutline(corners: Float, horizontalInset: Int = 0, roundBottom: Boolean = true) {
    outlineProvider = object: ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(
                horizontalInset,
                0,
                view.width - horizontalInset,
                view.height + if (roundBottom) 0 else corners.toInt(),
                corners
            )
        }
    }
}