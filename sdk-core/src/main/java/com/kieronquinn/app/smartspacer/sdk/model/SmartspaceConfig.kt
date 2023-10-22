package com.kieronquinn.app.smartspacer.sdk.model

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class SmartspaceConfig(
    /**
     *  The least number of smartspace targets expected to be predicted by the backend. The backend
     *  will always try to satisfy this threshold but it is not guaranteed to always meet it.
     */
    val smartspaceTargetCount: Int,
    /**
     *  A [uiSurface] is the name of the surface which will be used to display the cards.
     */
    val uiSurface: UiSurface,
    /**
     *  Package name of the client
     */
    val packageName: String,
    /**
     *  Send other client UI configurations in extras.
     *
     *  This can include:
     *
     *  - Desired maximum update frequency (For example 1 minute update frequency for AoD, 1 second
     *  update frequency for home screen etc).
     *  - Request to get periodic updates
     *  - Request to support multiple clients for the same UISurface.
     */
    val extras: Bundle? = null
): Parcelable {

    companion object {
        private const val KEY_SMARTSPACE_TARGET_COUNT = "smartspace_target_count"
        private const val KEY_UI_SURFACE ="ui_surface"
        private const val KEY_PACKAGE_NAME = "package_name"
        private const val KEY_EXTRAS = "extras"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getInt(KEY_SMARTSPACE_TARGET_COUNT),
        UiSurface.from(bundle.getString(KEY_UI_SURFACE)!!),
        bundle.getString(KEY_PACKAGE_NAME)!!,
        bundle.getBundle(KEY_EXTRAS)
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle(): Bundle {
        return bundleOf(
            KEY_SMARTSPACE_TARGET_COUNT to smartspaceTargetCount,
            KEY_UI_SURFACE to uiSurface.surface,
            KEY_PACKAGE_NAME to packageName,
            KEY_EXTRAS to extras
        )
    }

}
