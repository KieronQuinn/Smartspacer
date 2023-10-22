package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.complications

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemRestoreComplicationBinding
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.complications.RestoreComplicationsAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class RestoreComplicationsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<Item.Complication>,
    private val onComplicationClicked: (Item.Complication) -> Unit
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView) {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)
    private val monet = MonetCompat.getInstance()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRestoreComplicationBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder) {
        val item = items[position]
        val background = monet.getPrimaryColor(binding.root.context, !binding.root.context.isDarkMode)
        binding.root.backgroundTintList = ColorStateList.valueOf(background)
        val isCompatible = item.compatibilityState !is CompatibilityState.Incompatible
        binding.root.isEnabled = isCompatible
        binding.complicationName.text = item.label
        val description = if(isCompatible){
            item.description
        }else{
            (item.compatibilityState as CompatibilityState.Incompatible).reason
                ?: binding.root.context.getString(R.string.complications_add_incompatible_generic)
        }
        binding.complicationDescription.isVisible = description != null
        binding.complicationDescription.text = description
        glide.load(item.icon)
            .placeholder(binding.complicationIcon.drawable)
            .into(binding.complicationIcon)
        binding.root.alpha = if(isCompatible) 1f else 0.5f
        if(isCompatible){
            whenResumed {
                binding.root.onClicked().collect {
                    onComplicationClicked(item)
                }
            }
        }else{
            binding.root.setOnClickListener(null)
        }
        Unit
    }

    data class ViewHolder(val binding: ItemRestoreComplicationBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}