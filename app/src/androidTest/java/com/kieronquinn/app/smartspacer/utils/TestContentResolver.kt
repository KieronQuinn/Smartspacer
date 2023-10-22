package com.kieronquinn.app.smartspacer.utils

import android.database.ContentObserver
import android.net.Uri

class TestContentResolver {

    private val listeners = HashMap<ContentObserver, Uri>()

    fun registerContentObserver(uri: Uri, notifyForDescendants: Boolean, observer: ContentObserver) {
        listeners[observer] = uri
    }

    fun unregisterContentObserver(observer: ContentObserver) {
        listeners.remove(observer)
    }

    fun notifyChange(uri: Uri) {
        listeners.filter { it.value == uri }.forEach {
            it.key.onChange(false)
        }
    }

}