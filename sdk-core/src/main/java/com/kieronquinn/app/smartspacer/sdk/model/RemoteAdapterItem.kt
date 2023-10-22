package com.kieronquinn.app.smartspacer.sdk.model

import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.copy
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat

/**
 *  Represents a remote item in the AdapterView's adapter. Contains [remoteViews], the views
 *  normally inflated into the list item, and [onClickResponses], a list of [RemoteOnClickResponse]s
 *  that would normally be applied to the views.
 */
data class RemoteAdapterItem(
    val remoteViews: RemoteViews,
    val onClickResponses: List<RemoteOnClickResponse>
) {

    companion object {
        private const val KEY_REMOTE_VIEWS = "remote_views"
        private const val KEY_ON_CLICK_RESPONSES = "on_click_responses"
    }

    constructor(bundle: Bundle): this(
        bundle.getParcelableCompat(KEY_REMOTE_VIEWS, RemoteViews::class.java)!!,
        bundle.getParcelableArrayListCompat(KEY_ON_CLICK_RESPONSES, Bundle::class.java)!!.map {
            RemoteOnClickResponse(it)
        }
    )

    fun toBundle() = bundleOf(
        KEY_REMOTE_VIEWS to remoteViews.copy(),
        KEY_ON_CLICK_RESPONSES to onClickResponses.map { it.toBundle() }
    )

}