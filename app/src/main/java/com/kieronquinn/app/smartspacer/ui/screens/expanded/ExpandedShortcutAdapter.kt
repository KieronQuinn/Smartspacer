package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.databinding.ItemExpandedShortcutBinding
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcut
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.BaseShortcut
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.BaseShortcut.ItemType
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts.Shortcut
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView.ViewHolder
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

class ExpandedShortcutAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<BaseShortcut>,
    val tintColour: Int,
    private val onAppShortcutClicked: (AppShortcut) -> Unit,
    private val onShortcutClicked: (Shortcut) -> Unit
): LifecycleAwareRecyclerView.Adapter<ExpandedShortcutAdapter.ViewHolder>(recyclerView) {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val glide = Glide.with(recyclerView.context)

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].itemType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(ItemType.values()[viewType]){
            ItemType.APP_SHORTCUT -> {
                ViewHolder.AppShortcut(
                    ItemExpandedShortcutBinding.inflate(layoutInflater, parent, false)
                )
            }
            ItemType.SHORTCUT -> {
                ViewHolder.Shortcut(
                    ItemExpandedShortcutBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder) {
            is ViewHolder.AppShortcut -> holder.setup(items[position] as AppShortcut)
            is ViewHolder.Shortcut -> holder.setup(items[position] as Shortcut)
        }
    }

    private fun ViewHolder.AppShortcut.setup(appShortcut: AppShortcut) {
        binding.itemExpandedShortcutLabel.text = appShortcut.title
        binding.itemExpandedShortcutIcon.clipToOutline = true
        glide.load(appShortcut)
            .placeholder(binding.itemExpandedShortcutIcon.drawable)
            .into(binding.itemExpandedShortcutIcon)
        whenResumed {
            binding.root.onClicked().collect {
                onAppShortcutClicked(appShortcut)
            }
        }
    }

    private fun ViewHolder.Shortcut.setup(shortcut: Shortcut) {
        binding.itemExpandedShortcutLabel.text = shortcut.label
        binding.itemExpandedShortcutLabel.isVisible = shortcut.label != null
        val icon = shortcut.icon
        if(icon != null) {
            binding.itemExpandedShortcutIcon.isVisible = true
            binding.itemExpandedShortcutIcon.clipToOutline = true
            binding.itemExpandedShortcutIcon.setImageIcon(icon.icon)
            val tint = if (icon.shouldTint) {
                binding.root.context.getAttrColor(android.R.attr.textColorPrimary)
            } else null
            binding.itemExpandedShortcutIcon.imageTintList =
                tint?.let { ColorStateList.valueOf(it) }
        }else{
            binding.itemExpandedShortcutIcon.isVisible = false
        }
        whenResumed {
            binding.root.onClicked().collect {
                onShortcutClicked(shortcut)
            }
        }
    }

    sealed class ViewHolder(
        open val binding: ViewBinding
    ): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class AppShortcut(override val binding: ItemExpandedShortcutBinding): ViewHolder(binding)
        data class Shortcut(override val binding: ItemExpandedShortcutBinding): ViewHolder(binding)
    }

}