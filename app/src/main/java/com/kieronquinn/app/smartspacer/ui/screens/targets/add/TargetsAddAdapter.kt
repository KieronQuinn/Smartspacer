package com.kieronquinn.app.smartspacer.ui.screens.targets.add

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemTargetAddAppBinding
import com.kieronquinn.app.smartspacer.databinding.ItemTargetAddTargetBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.targets.add.TargetsAddAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class TargetsAddAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    private val onExpandClicked: (Item.App) -> Unit,
    private val onTargetClicked: (target: Item.Target) -> Unit,
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
                    if(oldItem is Item.Target && newItem is Item.Target) {
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
                ItemTargetAddAppBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.TARGET -> ViewHolder.Target(
                ItemTargetAddTargetBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.App -> holder.setup(currentList[position] as Item.App)
            is ViewHolder.Target -> holder.setup(currentList[position] as Item.Target)
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
        targetAddAppName.text = item.label
        targetAddAppArrow.rotation = if(item.isExpanded) 180f else 0f
        root.setOnClickListener {
            targetAddAppArrow.callOnClick()
        }
        setCardCornerRadius(item.isExpanded)
        whenResumed {
            targetAddAppArrow.onClicked().collect {
                if(item.isExpanded){
                    targetAddAppArrow.animate().rotation(0f).start()
                }else{
                    targetAddAppArrow.animate().rotation(180f).start()
                }
                setCardCornerRadius(!item.isExpanded)
                onExpandClicked(item)
            }
        }
        glide.load(PackageIcon(item.packageName))
            .placeholder(targetAddAppIcon.drawable)
            .into(targetAddAppIcon)
    }

    private fun ViewHolder.Target.setup(item: Item.Target) = with(binding) {
        val isCompatible = item.compatibilityState == CompatibilityState.Compatible
        root.isEnabled = isCompatible
        root.shapeAppearanceModel = if(item.isLastTarget){
            bottomRoundedShapeAppearance
        }else noneRoundedShapeAppearance
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        targetAddTargetName.text = item.label
        targetAddTargetDescription.text = if(isCompatible) {
            item.description
        }else{
            (item.compatibilityState as? CompatibilityState.Incompatible)?.reason
                ?: root.context.getString(R.string.targets_add_incompatible_generic)
        }
        targetAddTargetName.alpha = if(isCompatible) 1f else 0.5f
        targetAddTargetDescription.alpha = if(isCompatible) 1f else 0.5f
        targetAddTargetIcon.alpha = if(isCompatible) 1f else 0.5f
        glide.load(item.icon)
            .placeholder(targetAddTargetIcon.drawable)
            .into(targetAddTargetIcon)
        whenResumed {
            if(!isCompatible) {
                root.setOnClickListener(null)
                return@whenResumed
            }
            root.onClicked().collect {
                onTargetClicked(item)
            }
        }
    }

    sealed class ViewHolder(
        open val binding: ViewBinding
    ): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class App(override val binding: ItemTargetAddAppBinding): ViewHolder(binding)
        data class Target(override val binding: ItemTargetAddTargetBinding): ViewHolder(binding)
    }

}