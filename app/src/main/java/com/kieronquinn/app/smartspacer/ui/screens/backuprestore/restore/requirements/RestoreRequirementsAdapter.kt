package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemRestoreRequirementBinding
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class RestoreRequirementsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<Item>,
    private val onRequirementClicked: (Item) -> Unit
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView) {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)
    private val monet = MonetCompat.getInstance()
    private val invertedLabel = recyclerView.context
        .getString(R.string.restore_restore_requirements_description_inverted)

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRestoreRequirementBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder) {
        val item = items[position]
        val background = monet.getPrimaryColor(binding.root.context, !binding.root.context.isDarkMode)
        binding.root.backgroundTintList = ColorStateList.valueOf(background)
        val isCompatible = item.compatibilityState !is CompatibilityState.Incompatible
        binding.root.isEnabled = isCompatible
        binding.requirementName.text = item.label
        val description = if(isCompatible){
            item.description.appendInvertedIfRequired(item.invert)
        }else{
            (item.compatibilityState as CompatibilityState.Incompatible).reason
                ?: binding.root.context.getString(R.string.requirements_add_incompatible_generic)
        }
        binding.requirementDescription.isVisible = description != null
        binding.requirementDescription.text = description
        binding.requirementFor.isVisible = item.requirementForLabel != null
        binding.requirementFor.text = binding.root.context.getString(
            R.string.restore_requirements_for,
            item.requirementForLabel
        )
        glide.load(item.icon)
            .placeholder(binding.requirementIcon.drawable)
            .into(binding.requirementIcon)
        binding.root.alpha = if(isCompatible) 1f else 0.5f
        if(isCompatible){
            whenResumed {
                binding.root.onClicked().collect {
                    onRequirementClicked(item)
                }
            }
        }else{
            binding.root.setOnClickListener(null)
        }
        Unit
    }

    private fun CharSequence.appendInvertedIfRequired(inverted: Boolean): CharSequence {
        return if(inverted) {
            "$this $invertedLabel"
        }else this
    }

    data class ViewHolder(val binding: ItemRestoreRequirementBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}