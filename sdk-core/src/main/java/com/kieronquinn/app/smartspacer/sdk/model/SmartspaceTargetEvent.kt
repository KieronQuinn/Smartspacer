package com.kieronquinn.app.smartspacer.sdk.model

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class SmartspaceTargetEvent(
    val smartspaceTarget: SmartspaceTarget?,
    val smartspaceActionId: String?,
    val eventType: Int
): Parcelable {

    companion object {
        /**
         * User interacted with the target.
         */
        const val EVENT_TARGET_INTERACTION = 1

        /**
         * Smartspace target was brought into view.
         */
        const val EVENT_TARGET_SHOWN = 2

        /**
         * Smartspace target went out of view.
         */
        const val EVENT_TARGET_HIDDEN = 3

        /**
         * A dismiss action was issued by the user.
         */
        const val EVENT_TARGET_DISMISS = 4

        /**
         * A block action was issued by the user.
         */
        const val EVENT_TARGET_BLOCK = 5

        /**
         * The Ui surface came into view.
         */
        const val EVENT_UI_SURFACE_SHOWN = 6

        /**
         * The Ui surface went out of view.
         */
        const val EVENT_UI_SURFACE_HIDDEN = 7

        private const val KEY_SMARTSPACE_TARGET = "smartspace_target"
        private const val KEY_SMARTSPACE_ACTION_ID = "smartspace_action_id"
        private const val KEY_EVENT_TYPE = "event_type"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getBundle(KEY_SMARTSPACE_TARGET)?.let { SmartspaceTarget(it) },
        bundle.getString(KEY_SMARTSPACE_ACTION_ID),
        bundle.getInt(KEY_EVENT_TYPE)
    )

    fun toBundle(): Bundle {
        return bundleOf(
            KEY_SMARTSPACE_TARGET to smartspaceTarget?.toBundle(),
            KEY_SMARTSPACE_ACTION_ID to smartspaceActionId,
            KEY_EVENT_TYPE to eventType
        )
    }

}
