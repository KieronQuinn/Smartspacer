package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.targets

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemRestoreTargetBinding
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.targets.RestoreTargetsAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class RestoreTargetsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<Item.Target>,
    private val onTargetClicked: (Item.Target) -> Unit
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView) {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)
    private val monet = MonetCompat.getInstance()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRestoreTargetBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder) {
        val item = items[position]
        val background = monet.getPrimaryColor(binding.root.context, !binding.root.context.isDarkMode)
        binding.root.backgroundTintList = ColorStateList.valueOf(background)
        val isCompatible = item.compatibilityState !is CompatibilityState.Incompatible
        binding.root.isEnabled = isCompatible
        binding.targetName.text = item.label
        val description = if(isCompatible){
            item.description
        }else{
            (item.compatibilityState as CompatibilityState.Incompatible).reason
                ?: binding.root.context.getString(R.string.targets_add_incompatible_generic)
        }
        binding.targetDescription.isVisible = description != null
        binding.targetDescription.text = description
        glide.load(item.icon)
            .placeholder(binding.targetIcon.drawable)
            .into(binding.targetIcon)
        binding.root.alpha = if(isCompatible) 1f else 0.5f
        if(isCompatible){
            whenResumed {
                binding.root.onClicked().collect {
                    onTargetClicked(item)
                }
            }
        }else{
            binding.root.setOnClickListener(null)
        }
        Unit
    }
    
    data class ViewHolder(val binding: ItemRestoreTargetBinding): 
        LifecycleAwareRecyclerView.ViewHolder(binding.root)
    
}