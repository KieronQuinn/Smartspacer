package com.kieronquinn.app.smartspacer.ui.screens.base.manager

import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerViewModel.BaseHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import java.util.Collections

abstract class BaseManagerAdapter<T: BaseHolder, V: ViewHolder>(
    recyclerView: LifecycleAwareRecyclerView,
    open var items: List<ItemHolder<T>>
): LifecycleAwareRecyclerView.Adapter<V>(recyclerView) {

    abstract fun isSelected(item: T): Boolean
    abstract fun setSelected(item: T, selected: Boolean)

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = items.size

    fun moveItem(indexFrom: Int, indexTo: Int): Boolean {
        if (indexFrom < indexTo) {
            for (i in indexFrom until indexTo) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in indexFrom downTo indexTo + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(indexFrom, indexTo)
        return true
    }

    fun clearSelection() {
        items.forEachIndexed { index, item ->
            item.let {
                val itemHolderItem = (it as? ItemHolder.Item)?.item ?: return@let
                if (isSelected(itemHolderItem)) {
                    setSelected(itemHolderItem, false)
                    notifyItemChanged(index)
                }
            }
        }
    }

    abstract class ViewHolder(
        open val binding: ViewBinding
    ): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        fun onRowSelectionChange(isSelected: Boolean) {
            binding.root.alpha = if (isSelected) 0.5f else 1f
        }
    }

    sealed class ItemHolder<T>(val type: Type) {

        data class NativeStartReminder<T>(
            val onClick: () -> Unit,
            val onDismissClick: () -> Unit
        ): ItemHolder<T>(Type.NATIVE_START_REMINDER) {
            override fun getId(): Long {
                return -1
            }
        }

        data class DonatePrompt<T>(
            val onClick: () -> Unit,
            val onDismissClick: () -> Unit
        ): ItemHolder<T>(Type.DONATE_PROMPT) {
            override fun getId(): Long {
                return -2
            }
        }

        data class Item<T>(val item: T, private val id: Long): ItemHolder<T>(Type.ITEM) {
            override fun getId(): Long {
                return id
            }
        }

        abstract fun getId(): Long

        enum class Type {
            NATIVE_START_REMINDER,
            DONATE_PROMPT,
            ITEM
        }
    }

}