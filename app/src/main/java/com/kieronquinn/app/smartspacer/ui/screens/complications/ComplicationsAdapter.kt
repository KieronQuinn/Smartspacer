package com.kieronquinn.app.smartspacer.ui.screens.complications

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemComplicationBinding
import com.kieronquinn.app.smartspacer.databinding.ItemDonatePromptBinding
import com.kieronquinn.app.smartspacer.databinding.ItemNativeStartReminderBinding
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter
import com.kieronquinn.app.smartspacer.ui.screens.complications.ComplicationsAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.complications.ComplicationsViewModel.ComplicationHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.onLongClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat

class ComplicationsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<ItemHolder<ComplicationHolder>>,
    private val onDragHandleLongPress: (RecyclerView.ViewHolder) -> Unit,
    private val onComplicationClicked: (ComplicationHolder) -> Unit
): BaseManagerAdapter<ComplicationHolder, ViewHolder>(
    recyclerView, items
) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
    private val monet = MonetCompat.getInstance()
    private val glide = Glide.with(recyclerView.context)

    override fun isSelected(item: ComplicationHolder): Boolean {
        return item.isSelected
    }

    override fun setSelected(item: ComplicationHolder, selected: Boolean) {
        item.isSelected = selected
    }

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
                ViewHolder.ItemComplication(
                    ItemComplicationBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.ItemComplication -> {
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

    private fun ViewHolder.ItemComplication.setup(
        item: ComplicationHolder,
        viewHolder: ViewHolder
    ) = with(binding) {
        glide.load(item.info.icon)
            .placeholder(complicationIcon.drawable)
            .into(complicationIcon)
        complicationName.text = item.info.label
        complicationDescription.text = if(item.info.compatibilityState == com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState.Compatible) {
            item.info.description
        }else{
            root.context.getString(R.string.complication_not_available)
        }
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        whenResumed {
            root.onClicked().collect {
                onComplicationClicked(item)
            }
        }
        whenResumed {
            complicationDragHandle.onLongClicked().collect {
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
        data class ItemComplication(override val binding: ItemComplicationBinding): ViewHolder(binding)
        data class NativeStartReminder(
            override val binding: ItemNativeStartReminderBinding
        ): ViewHolder(binding)
        data class DonatePrompt(
            override val binding: ItemDonatePromptBinding
        ): ViewHolder(binding)
    }

}