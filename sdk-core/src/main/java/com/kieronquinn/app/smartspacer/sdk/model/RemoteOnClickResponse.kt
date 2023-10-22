package com.kieronquinn.app.smartspacer.sdk.model

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.model.RemoteOnClickResponse.RemoteResponse
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat

/**
 *  Represents a [RemoteResponse] that is normally applied to an [id]. This allows you to extract
 *  fill in [Intent]s from RemoteViews.
 */
data class RemoteOnClickResponse(val id: String, val response: RemoteResponse) {

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_RESPONSE = "response"
    }

    constructor(bundle: Bundle): this(
        bundle.getString(KEY_ID)!!,
        RemoteResponse(bundle.getBundle(KEY_RESPONSE)!!)
    )

    fun toBundle() = bundleOf(
        KEY_ID to id,
        KEY_RESPONSE to response.toBundle()
    )

    /**
     *  Equivalent to [android.widget.RemoteViews.RemoteResponse], but with parceling methods
     *  exposed (writing to a Bundle).
     */
    data class RemoteResponse(
        val interactionType: Int,
        val pendingIntent: PendingIntent?,
        val fillInIntent: Intent?,
        val viewIds: List<Int>,
        val elementNames: ArrayList<String>
    ) {

        companion object {
            private const val KEY_INTERACTION_TYPE = "interaction_type"
            private const val KEY_PENDING_INTENT = "pending_intent"
            private const val KEY_FILL_IN_INTENT = "fill_in_intent"
            private const val KEY_VIEW_IDS = "view_ids"
            private const val KEY_ELEMENT_NAMES = "element_names"

            const val INTERACTION_TYPE_CLICK = 0
            const val INTERACTION_TYPE_CHECKED_CHANGE = 1
        }

        constructor(bundle: Bundle): this(
            bundle.getInt(KEY_INTERACTION_TYPE),
            bundle.getParcelableCompat(KEY_PENDING_INTENT, PendingIntent::class.java),
            bundle.getParcelableCompat(KEY_FILL_IN_INTENT, Intent::class.java),
            bundle.getIntegerArrayList(KEY_VIEW_IDS) ?: emptyList(),
            bundle.getStringArrayList(KEY_ELEMENT_NAMES) ?: ArrayList()
        )

        fun toBundle() = bundleOf(
            KEY_INTERACTION_TYPE to interactionType,
            KEY_PENDING_INTENT to pendingIntent,
            KEY_FILL_IN_INTENT to fillInIntent,
            KEY_VIEW_IDS to viewIds,
            KEY_ELEMENT_NAMES to elementNames
        )

    }

}