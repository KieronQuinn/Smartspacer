package com.kieronquinn.app.smartspacer.ui.screens.base.manager

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerViewModel.BaseHolder
import com.kieronquinn.app.smartspacer.ui.screens.complications.ComplicationsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsAdapter

abstract class BaseManagerItemTouchHelperCallback<T: BaseHolder, VH: ViewHolder>: ItemTouchHelper.Callback() {

    abstract val adapter: BaseManagerAdapter<T, VH>
    abstract val viewModel: BaseManagerViewModel<T>

    private var startPos = -1

    override fun isLongPressDragEnabled() = false
    override fun isItemViewSwipeEnabled() = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
    }

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if(target is TargetsAdapter.ViewHolder.DonatePrompt){
            return false
        }
        if(target is TargetsAdapter.ViewHolder.NativeStartReminder){
            return false
        }
        if(target is ComplicationsAdapter.ViewHolder.NativeStartReminder){
            return false
        }
        if(target is ComplicationsAdapter.ViewHolder.DonatePrompt){
            return false
        }
        return super.canDropOver(recyclerView, current, target)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val startPos = viewHolder.adapterPosition
        val endPos = target.adapterPosition
        val result = adapter.moveItem(startPos, endPos)
        adapter.clearSelection()
        return result
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        //No-op
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            startPos = viewHolder?.adapterPosition ?: -1
            (viewHolder as? ViewHolder)?.onRowSelectionChange(true)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (startPos != -1) {
            viewModel.moveItem(startPos, viewHolder.adapterPosition)
            startPos = -1
        }
        (viewHolder as? ViewHolder)?.onRowSelectionChange(false)
    }

}