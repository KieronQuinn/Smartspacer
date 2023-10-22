package com.kieronquinn.app.smartspacer.utils.extensions

import android.media.session.PlaybackState
import android.os.Build

private val ACTIVE_STATES = arrayOf(
    PlaybackState.STATE_BUFFERING,
    PlaybackState.STATE_CONNECTING,
    PlaybackState.STATE_FAST_FORWARDING,
    PlaybackState.STATE_PLAYING,
    PlaybackState.STATE_REWINDING,
    PlaybackState.STATE_SKIPPING_TO_NEXT,
    PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
    PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM
)

fun PlaybackState.isActiveCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isActive
    } else {
        ACTIVE_STATES.contains(state)
    }
}