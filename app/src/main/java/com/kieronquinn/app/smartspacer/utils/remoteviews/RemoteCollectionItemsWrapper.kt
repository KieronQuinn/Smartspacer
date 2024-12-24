package com.kieronquinn.app.smartspacer.utils.remoteviews

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews.RemoteCollectionItems
import androidx.annotation.RequiresApi
import com.kieronquinn.app.smartspacer.sdk.IRemoteAdapter
import com.kieronquinn.app.smartspacer.sdk.model.RemoteAdapterItem
import com.kieronquinn.app.smartspacer.sdk.model.RemoteOnClickResponse
import com.kieronquinn.app.smartspacer.utils.extensions.extractOnClickResponse
import com.kieronquinn.app.smartspacer.utils.extensions.getActionsIncludingNested
import com.kieronquinn.app.smartspacer.utils.extensions.getResourceNameOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.isOnClickResponse
import com.kieronquinn.app.smartspacer.utils.extensions.toRemoteResponse

@RequiresApi(Build.VERSION_CODES.S)
class RemoteCollectionItemsWrapper(
    private val context: Context,
    private val items: RemoteCollectionItems
): IRemoteAdapter.Stub() {

    override fun getCount(): Int {
        return items.itemCount
    }

    override fun getViewAt(index: Int): Bundle {
        val remoteViews = items.getItemView(index)
        val actions = remoteViews.getActionsIncludingNested()
            .filter { it.isOnClickResponse() }.map { it.extractOnClickResponse() }.mapNotNull {
                if(it.first == 0) return@mapNotNull null
                val viewId = context.resources.getResourceNameOrNull(it.first)
                    ?: View.NO_ID.toString()
                RemoteOnClickResponse(viewId, it.second.toRemoteResponse())
            }
        return RemoteAdapterItem(remoteViews, actions).toBundle()
    }

}