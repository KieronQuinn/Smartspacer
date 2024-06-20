package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedComplicationsBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedFooterBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedRemoteviewsBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedRemovedWidgetBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedSearchBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedShortcutsBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedSpacerBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedStatusBarSpaceBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedTargetBinding
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedWidgetBinding
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcut
import com.kieronquinn.app.smartspacer.model.doodle.DoodleImage
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.ui.views.appwidget.ExpandedAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.onLongClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.core.component.KoinComponent

interface BaseExpandedAdapter: KoinComponent {

    val isRearrange: Boolean
    val expandedRepository: ExpandedRepository

    fun ViewHolder.Widget.setup(
        context: Context,
        availableWidth: Int,
        widget: Item.Widget,
        sessionId: String,
        handler: SmartspaceTargetInteractionListener
    ) = with(binding) {
        val tintColour = getTintColour(widget.isDark)
        itemExpandedWidgetDragLayer.isVisible = isRearrange
        itemExpandedWidgetConfigure.setTextColor(tintColour)
        val longClickListener = View.OnLongClickListener {
            if(widget.isCustom && this@BaseExpandedAdapter is ExpandedAdapter){
                onCustomWidgetLongClicked(it, widget)
            }else{
                onWidgetLongClicked(this@setup, widget.appWidgetId)
            }
        }
        if(widget.appWidgetId != null){
            itemExpandedWidgetConfigure.isVisible = false
            itemExpandedWidgetConfigureSpace.isVisible = false
            itemExpandedWidgetContainer.isVisible = true
            itemExpandedWidgetContainer.run {
                removeAllViews()
                val appWidgetHostView = expandedRepository.createHost(
                    context,
                    availableWidth,
                    widget,
                    sessionId,
                    handler
                )
                appWidgetHostView.clipChildren = false
                appWidgetHostView.clipToPadding = false
                appWidgetHostView.enforceRoundedCorners = widget.roundCorners
                appWidgetHostView.removeFromParentIfNeeded()
                appWidgetHostView.setOnLightBackground(widget.isDark)
                addView(appWidgetHostView)
                appWidgetHostView.setAppWidget(widget.appWidgetId, widget.provider)
                setOnLongClickListener(longClickListener)
            }
        }else{
            itemExpandedWidgetConfigure.isVisible = true
            itemExpandedWidgetConfigureSpace.isVisible = true
            itemExpandedWidgetContainer.isVisible = false
            whenResumed {
                itemExpandedWidgetConfigure.onClicked().collect {
                    onConfigureWidgetClicked(widget.provider, widget.id, widget.config)
                }
            }
        }
        whenResumed {
            itemExpandedWidgetDragLayer.onLongClicked(false).collect {
                if(widget.appWidgetId != null) {
                    longClickListener.onLongClick(it)
                }
            }
        }
        whenResumed {
            itemExpandedWidgetRoot.onLongClicked(false).collect {
                if(widget.appWidgetId != null) {
                    longClickListener.onLongClick(it)
                }
            }
        }
    }

    fun ViewHolder.RemovedWidget.setup(
        widget: Item.RemovedWidget,
        isRearrange: Boolean = false
    ) = with(binding) {
        val tintColour = getTintColour(widget.isDark)
        itemExpandedWidgetDragLayer.isVisible = isRearrange
        itemExpandedWidgetRemove.setTextColor(tintColour)
        itemExpandedWidgetRemove.text = if(isRearrange){
            root.context.getString(R.string.expanded_widget_deleted_rearrange)
        }else{
            root.context.getString(R.string.expanded_widget_deleted)
        }
        whenResumed {
            itemExpandedWidgetRemove.onClicked().collect {
                onDeleteWidgetClicked(widget)
            }
        }
        whenResumed {
            itemExpandedWidgetDragLayer.onLongClicked().collect {
                onWidgetLongClicked(this@setup, widget.appWidgetId)
            }
        }
    }

    fun ViewHolder.Widget.destroy() = with(binding) {
        itemExpandedWidgetContainer.children.filterIsInstance<ExpandedAppWidgetHostView>().forEach {
            itemExpandedWidgetContainer.removeView(it)
        }
    }

    fun onDetached(sessionId: String?) {
        expandedRepository.destroyHosts(sessionId)
    }

    fun onCustomWidgetLongClicked(view: View, widget: Item.Widget): Boolean
    fun onWidgetLongClicked(viewHolder: ViewHolder, appWidgetId: Int?): Boolean
    fun onConfigureWidgetClicked(provider: AppWidgetProviderInfo, id: String?, config: CustomExpandedAppWidgetConfig?)
    fun onDeleteWidgetClicked(widget: Item.RemovedWidget)

    interface ExpandedAdapterListener {
        fun onConfigureWidgetClicked(info: AppWidgetProviderInfo, id: String?, config: CustomExpandedAppWidgetConfig?)
        fun onShortcutClicked(shortcut: ExpandedState.Shortcuts.Shortcut)
        fun onSearchBoxClicked(searchApp: SearchApp)
        fun onSearchMicClicked(searchApp: SearchApp)
        fun onSearchLensClicked(searchApp: SearchApp)
        fun onDoodleClicked(doodleImage: DoodleImage)
        fun onAppShortcutClicked(appShortcut: AppShortcut)
        fun onAddWidgetClicked()
        fun onCustomWidgetLongClicked(view: View, widget: Item.Widget): Boolean
        fun onWidgetLongClicked(viewHolder: RecyclerView.ViewHolder, appWidgetId: Int?): Boolean
        fun onWidgetDeleteClicked(widget: Item.RemovedWidget)
    }

    sealed class ViewHolder(
        open val binding: ViewBinding
    ): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class StatusBarSpace(override val binding: ItemExpandedStatusBarSpaceBinding): ViewHolder(binding)
        data class Search(override val binding: ItemExpandedSearchBinding): ViewHolder(binding)
        data class Target(override val binding: ItemExpandedTargetBinding): ViewHolder(binding)
        data class Complications(override val binding: ItemExpandedComplicationsBinding): ViewHolder(binding)
        data class RemoteViews(override val binding: ItemExpandedRemoteviewsBinding): ViewHolder(binding)
        data class Widget(override val binding: ItemExpandedWidgetBinding): ViewHolder(binding)
        data class RemovedWidget(override val binding: ItemExpandedRemovedWidgetBinding): ViewHolder(binding)
        data class Shortcuts(override val binding: ItemExpandedShortcutsBinding): ViewHolder(binding)
        data class Footer(override val binding: ItemExpandedFooterBinding): ViewHolder(binding)
        data class Spacer(override val binding: ItemExpandedSpacerBinding): ViewHolder(binding)
    }

    fun getTintColour(isDark: Boolean): Int {
        return if(isDark) Color.BLACK else Color.WHITE
    }

}