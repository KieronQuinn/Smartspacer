package com.kieronquinn.app.smartspacer.ui.screens.expanded.rearrange

import android.appwidget.AppWidgetProviderInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.flexbox.FlexboxLayoutManager
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedRemovedWidgetBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedWidgetBinding
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.ui.screens.expanded.BaseExpandedAdapter
import com.kieronquinn.app.smartspacer.ui.screens.expanded.BaseExpandedAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import org.koin.core.component.inject
import java.util.Collections

class ExpandedRearrangeAdapter(
    var items: List<Item>,
    recyclerView: LifecycleAwareRecyclerView,
    private val listener: BaseExpandedAdapter.ExpandedAdapterListener,
    private val getSpanPercent: (Item) -> Float,
    private val getAvailableWidth: () -> Int
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView), BaseExpandedAdapter, SmartspaceTargetInteractionListener {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)

    private val context = recyclerView.context.applicationContext
    override val expandedRepository by inject<ExpandedRepository>()

    override val isRearrange = true
    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.Type.entries[viewType]) {
            Item.Type.WIDGET -> {
                ViewHolder.Widget(
                    ItemExpandedWidgetBinding.inflate(layoutInflater, parent, false)
                )
            }
            Item.Type.REMOVED_WIDGET -> {
                ViewHolder.RemovedWidget(
                    ItemExpandedRemovedWidgetBinding.inflate(
                        layoutInflater, parent, false
                    )
                )
            }
            else -> throw RuntimeException("Invalid item type for rearrange screen")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val spanSize = getSpanPercent(item)
        val layoutParams = holder.binding.root.layoutParams as FlexboxLayoutManager.LayoutParams
        layoutParams.flexBasisPercent = spanSize
        when(holder) {
            is ViewHolder.Widget -> {
                holder.setup(
                    context,
                    getAvailableWidth(),
                    item as Item.Widget,
                    "rearrange",
                    this
                )
            }
            is ViewHolder.RemovedWidget -> {
                holder.setup(item as Item.RemovedWidget, true)
            }
            else -> {
                //No-op
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? ViewHolder.Widget)?.destroy()
    }

    override fun onConfigureWidgetClicked(
        provider: AppWidgetProviderInfo,
        id: String?,
        config: ExpandedRepository.CustomExpandedAppWidgetConfig?
    ) {
        listener.onConfigureWidgetClicked(provider, id, config)
    }

    override fun onDeleteWidgetClicked(widget: Item.RemovedWidget) {
        //No-op, cannot be accessed on this screen
    }

    override fun onInteraction(target: SmartspaceTarget, actionId: String?) {
        //No-op
    }

    override fun onLongPress(target: SmartspaceTarget): Boolean {
        return false
    }

    override fun onWidgetLongClicked(viewHolder: ViewHolder, appWidgetId: Int?): Boolean {
        return listener.onWidgetLongClicked(viewHolder, appWidgetId)
    }

    override fun onCustomWidgetLongClicked(view: View, widget: Item.Widget): Boolean {
        return listener.onCustomWidgetLongClicked(view, widget)
    }

    fun moveItem(indexFrom: Int, indexTo: Int): Boolean {
        if (indexFrom < indexTo) {
            for (i in indexFrom until indexTo) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in indexFrom downTo indexTo + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(indexFrom, indexTo)
        return true
    }

}