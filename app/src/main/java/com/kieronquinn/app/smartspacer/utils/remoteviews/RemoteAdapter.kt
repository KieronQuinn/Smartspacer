package com.kieronquinn.app.smartspacer.utils.remoteviews

import android.content.Context
import android.os.Bundle
import android.view.View
import com.kieronquinn.app.smartspacer.sdk.IRemoteAdapter
import com.kieronquinn.app.smartspacer.sdk.model.RemoteAdapterItem
import com.kieronquinn.app.smartspacer.sdk.model.RemoteOnClickResponse
import com.kieronquinn.app.smartspacer.utils.extensions.extractOnClickResponse
import com.kieronquinn.app.smartspacer.utils.extensions.getActionsIncludingSized
import com.kieronquinn.app.smartspacer.utils.extensions.getResourceNameOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.isOnClickResponse
import com.kieronquinn.app.smartspacer.utils.extensions.toRemoteResponse

class RemoteAdapter(
    private val context: Context,
    private val factoryWrapper: RemoteViewsFactoryWrapper
): IRemoteAdapter.Stub() {

    override fun getViewAt(index: Int): Bundle? {
        val remoteViews = factoryWrapper.getViewAt(index) ?: return null
        val actions = remoteViews.getActionsIncludingSized()
            .filter { it.isOnClickResponse() }.map { it.extractOnClickResponse() }.mapNotNull {
                if(it.first == 0) return@mapNotNull null
                val viewId = context.resources.getResourceNameOrNull(it.first)
                    ?: View.NO_ID.toString()
                RemoteOnClickResponse(viewId, it.second.toRemoteResponse())
            }
        return RemoteAdapterItem(remoteViews, actions).toBundle()
    }

    override fun getCount(): Int {
        return factoryWrapper.getCount()
    }
}