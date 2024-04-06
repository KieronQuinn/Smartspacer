package com.kieronquinn.app.smartspacer.ui.screens.expanded.rearrange

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ExpandedRearrangeItemTouchHelperCallback(
    private val viewModel: ExpandedRearrangeViewModel,
    private val adapter: ExpandedRearrangeAdapter
): ItemTouchHelper.Callback() {

    private var startPos = -1

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val startPos = viewHolder.adapterPosition
        val endPos = target.adapterPosition
        return adapter.moveItem(startPos, endPos)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            startPos = viewHolder?.adapterPosition ?: -1
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        //No-op
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (startPos != -1) {
            viewModel.moveItem(startPos, viewHolder.adapterPosition)
            startPos = -1
        }
    }

    override fun isLongPressDragEnabled() = false

}