package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedComplicationBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.setIcon
import com.kieronquinn.app.smartspacer.sdk.client.utils.setOnClick
import com.kieronquinn.app.smartspacer.sdk.client.utils.setText
import com.kieronquinn.app.smartspacer.sdk.client.utils.shouldHeaderTintIcon
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedComplicationAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession.Complications.Complication
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.setShadowEnabled

class ExpandedComplicationAdapter(
    context: Context,
    private var items: List<Complication>,
    private val tintColour: Int,
    private val showShadow: Boolean,
    private val interactionListener: SmartspaceTargetInteractionListener
): RecyclerView.Adapter<ViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemExpandedComplicationBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(val item = items[position]) {
            is Complication.Action -> holder.setup(item)
            is Complication.SubItemInfo -> holder.setup(item)
        }
    }

    private fun ViewHolder.setup(complication: Complication.Action) = with(binding) {
        expandedComplicationText.setShadowEnabled(showShadow)
        expandedComplicationIcon.setShadowEnabled(showShadow)
        expandedComplicationText.setText(complication.smartspaceAction.title, tintColour)
        val shouldTint = complication.parent.shouldHeaderTintIcon()
        val icon = complication.smartspaceAction.icon?.let { icon ->
            Icon(icon, shouldTint = shouldTint)
        }
        expandedComplicationIcon.setIcon(icon, tintColour)
        root.setOnClick(complication.parent, complication.smartspaceAction, interactionListener)
    }

    private fun ViewHolder.setup(complication: Complication.SubItemInfo) = with(binding) {
        expandedComplicationText.setShadowEnabled(showShadow)
        expandedComplicationIcon.setShadowEnabled(showShadow)
        expandedComplicationIcon.setIcon(complication.info.icon, tintColour)
        complication.info.text?.let { text -> expandedComplicationText.setText(text, tintColour) }
        root.setOnClick(complication.parent, complication.info.tapAction, interactionListener)
    }

    override fun getItemCount() = items.size

    fun update(items: List<Complication>) {
        this.items = items.toList()
        notifyDataSetChanged()
    }

    data class ViewHolder(val binding: ItemExpandedComplicationBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}