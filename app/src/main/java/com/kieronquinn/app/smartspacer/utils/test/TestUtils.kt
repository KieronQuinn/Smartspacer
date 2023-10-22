package com.kieronquinn.app.smartspacer.utils.test

import android.database.ContentObserver
import android.net.Uri

/**
 *  Override methods for methods mockk can't mock for some reason. These are only set in the tests.
 */
object TestUtils {

    var registerContentObserver: ((Uri, ContentObserver) -> Unit)? = null
    var unregisterContentObserver: ((ContentObserver) -> Unit)? = null

}