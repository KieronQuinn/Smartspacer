package com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.add

import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemRequirementAddAppBinding
import com.kieronquinn.app.smartspacer.databinding.ItemRequirementAddRequirementBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.add.TargetsRequirementsAddAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.add.TargetsRequirementsAddViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class TargetsRequirementsAddAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    private val onExpandClicked: (Item.App) -> Unit,
    private val onRequirementClicked: (authority: String, id: String, packageName: String, setupIntent: Intent?) -> Unit,
): LifecycleAwareRecyclerView.ListAdapter<Item, ViewHolder>(
    createDiffUtil(), recyclerView
) {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)
    private val monet = MonetCompat.getInstance()

    private val cornerRadius by lazy {
        recyclerView.context.resources.getDimension(R.dimen.margin_16)
    }

    private val bottomRoundedShapeAppearance by lazy {
        ShapeAppearanceModel.Builder()
            .setBottomLeftCorner(CornerFamily.ROUNDED, cornerRadius)
            .setBottomRightCorner(CornerFamily.ROUNDED, cornerRadius)
            .build()
    }

    private val topRoundedShapeAppearance by lazy {
        ShapeAppearanceModel.Builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
            .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
            .build()
    }

    private val allRoundedShapeAppearance by lazy {
        ShapeAppearanceModel.Builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
            .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
            .setBottomLeftCorner(CornerFamily.ROUNDED, cornerRadius)
            .setBottomRightCorner(CornerFamily.ROUNDED, cornerRadius)
            .build()
    }

    private val noneRoundedShapeAppearance by lazy {
        ShapeAppearanceModel.Builder().build()
    }

    companion object {
        fun createDiffUtil(): DiffUtil.ItemCallback<Item> {
            return object: DiffUtil.ItemCallback<Item>() {
                override fun areItemsTheSame(
                    oldItem: Item,
                    newItem: Item
                ): Boolean {
                    return oldItem::class.java == newItem::class.java
                }

                override fun areContentsTheSame(
                    oldItem: Item,
                    newItem: Item
                ): Boolean {
                    if(oldItem is Item.Requirement && newItem is Item.Requirement) {
                        return oldItem.authority == newItem.authority
                    }
                    if(oldItem is Item.App && newItem is Item.App) {
                        return oldItem.packageName == newItem.packageName
                    }
                    return false
                }

            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return currentList[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.Type.values()[viewType]){
            Item.Type.APP -> ViewHolder.App(
                ItemRequirementAddAppBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.REQUIREMENT -> ViewHolder.Requirement(
                ItemRequirementAddRequirementBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.App -> holder.setup(currentList[position] as Item.App)
            is ViewHolder.Requirement -> holder.setup(currentList[position] as Item.Requirement)
        }
    }

    private fun ViewHolder.App.setup(item: Item.App) = with(binding) {
        val setCardCornerRadius = { expanded: Boolean ->
            root.shapeAppearanceModel = if(expanded){
                topRoundedShapeAppearance
            }else{
                allRoundedShapeAppearance
            }
        }
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        requirementAddAppName.text = item.label
        requirementAddAppArrow.rotation = if(item.isExpanded) 180f else 0f
        root.setOnClickListener {
            requirementAddAppArrow.callOnClick()
        }
        setCardCornerRadius(item.isExpanded)
        whenResumed {
            requirementAddAppArrow.onClicked().collect {
                if(item.isExpanded){
                    requirementAddAppArrow.animate().rotation(0f).start()
                }else{
                    requirementAddAppArrow.animate().rotation(180f).start()
                }
                setCardCornerRadius(!item.isExpanded)
                onExpandClicked(item)
            }
        }
        glide.load(PackageIcon(item.packageName))
            .placeholder(requirementAddAppIcon.drawable)
            .into(requirementAddAppIcon)
    }

    private fun ViewHolder.Requirement.setup(item: Item.Requirement) = with(binding) {
        val isCompatible = item.compatibilityState == CompatibilityState.Compatible
        root.isEnabled = isCompatible
        root.shapeAppearanceModel = if(item.isLastRequirement){
            bottomRoundedShapeAppearance
        }else noneRoundedShapeAppearance
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        requirementAddRequirementName.text = item.label
        requirementAddRequirementDescription.text = if(isCompatible) {
            item.description
        }else{
            (item.compatibilityState as? CompatibilityState.Incompatible)?.reason
                ?: root.context.getString(R.string.requirements_add_incompatible_generic)
        }
        requirementAddRequirementName.alpha = if(isCompatible) 1f else 0.5f
        requirementAddRequirementDescription.alpha = if(isCompatible) 1f else 0.5f
        requirementAddRequirementIcon.alpha = if(isCompatible) 1f else 0.5f
        glide.load(item.icon)
            .placeholder(requirementAddRequirementIcon.drawable)
            .into(requirementAddRequirementIcon)
        whenResumed {
            if(!isCompatible) {
                root.setOnClickListener(null)
                return@whenResumed
            }
            root.onClicked().collect {
                onRequirementClicked(item.authority, item.id, item.packageName, item.setupIntent)
            }
        }
    }

    sealed class ViewHolder(
        open val binding: ViewBinding
    ): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class App(override val binding: ItemRequirementAddAppBinding): ViewHolder(binding)
        data class Requirement(override val binding: ItemRequirementAddRequirementBinding): ViewHolder(binding)
    }

}