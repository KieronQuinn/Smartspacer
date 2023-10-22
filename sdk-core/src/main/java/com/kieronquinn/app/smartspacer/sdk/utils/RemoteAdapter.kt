package com.kieronquinn.app.smartspacer.sdk.utils

import android.app.PendingIntent
import android.os.Bundle
import android.os.RemoteException
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.IRemoteAdapter
import com.kieronquinn.app.smartspacer.sdk.model.RemoteAdapterItem

/**
 *  Represents an adapter for an AdapterView within the RemoteViews for a widget. Smartspacer
 *  automatically connects to the Adapter service for the widget, and provides you with a connection
 *  to this for around 10 seconds after a call to your provider.
 *
 *  You can find out which view this adapter is for with [viewIdentifier], this is the String
 *  identifier for the ID in the format `$PACKAGE_NAME:id/$VIEW_ID`, if it available. Alternatively,
 *  [viewId] will contain the raw view ID, if it is available.
 *
 *  To load a [RemoteAdapterItem] for a given item, call [getViewAt]. To check the size of the items
 *  list for this adapter, call [getCount]. You cannot access the raw objects from the adapter,
 *  consistent with the original RemoteViewsAdapter this would normally be used for.
 */
class RemoteAdapter(
    private val remote: IRemoteAdapter,
    val adapterViewPendingIntent: PendingIntent?,
    val viewIdentifier: String?,
    val viewId: Int?
) {

    companion object {
        private const val KEY_REMOTE = "remote"
        private const val KEY_ADAPTER_VIEW_PENDING_INTENT = "adapter_view_pending_intent"
        private const val KEY_VIEW_IDENTIFIER = "view_identifier"
        private const val KEY_VIEW_ID = "view_id"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        IRemoteAdapter.Stub.asInterface(bundle.getBinder(KEY_REMOTE)),
        bundle.getParcelableCompat(KEY_ADAPTER_VIEW_PENDING_INTENT, PendingIntent::class.java),
        bundle.getString(KEY_VIEW_IDENTIFIER),
        bundle.getInt(KEY_VIEW_ID, -1).takeIf { it > 0 },
    )

    fun getCount(): Int {
        return withRemote { it.count } ?: 0
    }

    fun getViewAt(index: Int): RemoteAdapterItem? {
        return withRemote {
            RemoteAdapterItem(it.getViewAt(index) ?: return@withRemote null)
        }
    }

    private fun <T> withRemote(block: (IRemoteAdapter) -> T): T? {
        return try {
            block(remote)
        }catch (e: RemoteException) {
            null
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle() = bundleOf(
        KEY_REMOTE to remote.asBinder(),
        KEY_ADAPTER_VIEW_PENDING_INTENT to adapterViewPendingIntent,
        KEY_VIEW_IDENTIFIER to viewIdentifier,
        KEY_VIEW_ID to viewId
    )

}