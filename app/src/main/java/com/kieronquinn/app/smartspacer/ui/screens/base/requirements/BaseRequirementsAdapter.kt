package com.kieronquinn.app.smartspacer.ui.screens.base.requirements

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemRequirementBinding
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.RequirementHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

abstract class BaseRequirementsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    open var items: List<RequirementHolder>,
    private val onConfigureClicked: (item: RequirementHolder) -> Unit,
    private val onDeleteClicked: (item: RequirementHolder) -> Unit,
    private val onInvertClicked: (item: RequirementHolder) -> Unit
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)
    private val monet = MonetCompat.getInstance()

    override fun getItemId(position: Int): Long {
        return items[position].requirement.id.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.Requirement(
            ItemRequirementBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.Requirement -> holder.setup(items[position])
        }
    }

    private fun ViewHolder.Requirement.setup(requirement: RequirementHolder) = with(binding) {
        val isCompatible = requirement.compatibilityState == CompatibilityState.Compatible
        requirementName.text = requirement.label
        requirementDescription.text = when (requirement.compatibilityState) {
            CompatibilityState.Compatible -> {
                requirement.description
            }
            else -> {
                root.context.getString(R.string.requirement_not_available)
            }
        }
        requirementConfigure.isVisible = requirement.configurationIntent != null
        requirementDelete.isVisible = isCompatible
        requirementInvert.isVisible = isCompatible
        requirementInvert.isChecked = requirement.requirement.invert
        glide.load(requirement.icon)
            .placeholder(requirementIcon.drawable)
            .into(requirementIcon)
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        whenResumed {
            requirementConfigure.onClicked().collect {
                onConfigureClicked(requirement)
            }
        }
        whenResumed {
            requirementDelete.onClicked().collect {
                onDeleteClicked(requirement)
            }
        }
        whenResumed {
            requirementInvertClickable.onClicked().collect {
                onInvertClicked(requirement)
            }
        }
        whenResumed {
            root.onClicked().collect {
                if(!isCompatible) {
                    onDeleteClicked(requirement)
                }
            }
        }
    }

    sealed class ViewHolder(
        open val binding: ViewBinding
    ): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class Requirement(override val binding: ItemRequirementBinding): ViewHolder(binding)
    }
}