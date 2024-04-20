package com.kieronquinn.app.smartspacer.utils.extensions

import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun ViewPager2.onPageChanged() = callbackFlow {
    val callback = object: OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            trySend(position)
        }
    }
    registerOnPageChangeCallback(callback)
    awaitClose {
        unregisterOnPageChangeCallback(callback)
    }
}

fun ViewPager2.onPageScrolled() = callbackFlow {
    val callback = object: OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            trySend(Triple(position, positionOffset, positionOffsetPixels))
        }
    }
    registerOnPageChangeCallback(callback)
    awaitClose {
        unregisterOnPageChangeCallback(callback)
    }
}