package com.kieronquinn.app.smartspacer.ui.screens.targets

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemDonatePromptBinding
import com.kieronquinn.app.smartspacer.databinding.ItemNativeStartReminderBinding
import com.kieronquinn.app.smartspacer.databinding.ItemTargetBinding
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsViewModel.TargetHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.onLongClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class TargetsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<ItemHolder<TargetHolder>>,
    private val onDragHandleLongPress: (RecyclerView.ViewHolder) -> Unit,
    private val onTargetClicked: (TargetHolder) -> Unit
): BaseManagerAdapter<TargetHolder, ViewHolder>(
    recyclerView, items
) {

    init {
        setHasStableIds(true)
    }

    override fun isSelected(item: TargetHolder): Boolean {
        return item.isSelected
    }

    override fun setSelected(item: TargetHolder, selected: Boolean) {
        item.isSelected = selected
    }

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val monet = MonetCompat.getInstance()
    private val glide = Glide.with(recyclerView.context)

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].getId()
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(ItemHolder.Type.values()[viewType]){
            ItemHolder.Type.NATIVE_START_REMINDER -> {
                ViewHolder.NativeStartReminder(
                    ItemNativeStartReminderBinding.inflate(layoutInflater, parent, false)
                )
            }
            ItemHolder.Type.DONATE_PROMPT -> {
                ViewHolder.DonatePrompt(
                    ItemDonatePromptBinding.inflate(layoutInflater, parent, false)
                )
            }
            ItemHolder.Type.ITEM -> {
                ViewHolder.ItemTarget(ItemTargetBinding.inflate(layoutInflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.ItemTarget -> {
                val item = items[position] as ItemHolder.Item
                holder.setup(item.item, holder)
            }
            is ViewHolder.NativeStartReminder -> {
                val item = items[position] as ItemHolder.NativeStartReminder
                holder.setup(item)
            }
            is ViewHolder.DonatePrompt -> {
                val item = items[position] as ItemHolder.DonatePrompt
                holder.setup(item)
            }
        }
    }

    private fun ViewHolder.ItemTarget.setup(
        item: TargetHolder,
        viewHolder: ViewHolder
    ) = with(binding) {
        glide.load(item.info.icon)
            .placeholder(targetIcon.drawable)
            .into(targetIcon)
        targetName.text = item.info.label
        targetDescription.text = if(item.info.compatibilityState == CompatibilityState.Compatible){
            item.info.description
        }else{
            root.context.getString(R.string.target_not_available)
        }
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        whenResumed {
            root.onClicked().collect {
                onTargetClicked(item)
            }
        }
        whenResumed {
            targetDragHandle.onLongClicked().collect {
                onDragHandleLongPress(viewHolder)
            }
        }
    }

    private fun ViewHolder.NativeStartReminder.setup(
        item: ItemHolder.NativeStartReminder<*>
    ) = with(binding) {
        root.applyBackgroundTint(monet)
        binding.nativeStartReminderDismiss.setTextColor(monet.getAccentColor(root.context))
        whenResumed {
            binding.root.onClicked().collect {
                item.onClick()
            }
        }
        whenResumed {
            binding.nativeStartReminderDismiss.onClicked().collect {
                item.onDismissClick()
            }
        }
    }

    private fun ViewHolder.DonatePrompt.setup(
        item: ItemHolder.DonatePrompt<*>
    ) = with(binding) {
        root.applyBackgroundTint(monet)
        binding.donatePromptDismiss.setTextColor(monet.getAccentColor(root.context))
        whenResumed {
            binding.root.onClicked().collect {
                item.onClick()
            }
        }
        whenResumed {
            binding.donatePromptDismiss.onClicked().collect {
                item.onDismissClick()
            }
        }
    }

    sealed class ViewHolder(
        override val binding: ViewBinding
    ): BaseManagerAdapter.ViewHolder(binding) {
        data class ItemTarget(override val binding: ItemTargetBinding): ViewHolder(binding)
        data class NativeStartReminder(
            override val binding: ItemNativeStartReminderBinding
        ): ViewHolder(binding)
        data class DonatePrompt(
            override val binding: ItemDonatePromptBinding
        ): ViewHolder(binding)
    }

}