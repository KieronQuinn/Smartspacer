package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo

interface RemoteWidgetView<T: View> {

    val identifier: String?

    companion object {
        private const val KEY_CLASS = "class"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val KEY_IDENTIFIER = "identifier"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): RemoteWidgetView<*> {
            throw NotImplementedError()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @CallSuper
    fun toBundle(bundle: Bundle = Bundle()): Bundle {
        bundle.putString(KEY_CLASS, javaClass.simpleName)
        bundle.putString(KEY_IDENTIFIER, identifier)
        return bundle
    }

}