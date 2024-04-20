package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.kieronquinn.app.smartspacer.databinding.ItemWidgetPredictedWidgetBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewPagerAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.utils.extensions.TAP_DEBOUNCE
import com.kieronquinn.app.smartspacer.utils.extensions.getHeightSpan
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetColumnWidth
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetRowHeight
import com.kieronquinn.app.smartspacer.utils.extensions.getWidthSpan
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

class ExpandedAddWidgetBottomSheetViewPagerAdapter(
    context: Context,
    private val glide: RequestManager,
    private val widgetContext: Context,
    private val getAvailableWidth: () -> Int,
    private val items: List<Item.Widget>
): RecyclerView.Adapter<ViewHolder>(), BaseExpandedAddWidgetBottomSheetAdapter {

    private var onClickListener: ((widget: Item.Widget, spanX: Int, spanY: Int) -> Unit)? = null
    private val layoutInflater = LayoutInflater.from(context)

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemWidgetPredictedWidgetBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[holder.adapterPosition]
        val context = root.context
        val availableWidth = getAvailableWidth()
        val columnWidth = context.getWidgetColumnWidth(availableWidth)
        val rowHeight = context.getWidgetRowHeight(availableWidth)
        val spanX = item.info.getWidthSpan(columnWidth)
        val spanY = item.info.getHeightSpan(rowHeight)
        item.parent.icon?.let {
            widgetPredictedWidgetAppIcon.setImageIcon(it)
        } ?: run {
            glide.load(PackageIcon(item.parent.packageName)).into(widgetPredictedWidgetAppIcon)
        }
        setupWidget(
            widgetContext,
            glide,
            item.info,
            spanX,
            spanY,
            columnWidth,
            rowHeight,
            item.label,
            item.description,
            root,
            widgetPredictedWidgetImage,
            widgetPredictedWidgetContainer,
            widgetPredictedWidgetName,
            widgetPredictedWidgetDescription
        )
        root.setOnClickListener {
            onClickListener?.invoke(item, spanX, spanY)
        }
    }

    data class ViewHolder(
        val binding: ItemWidgetPredictedWidgetBinding
    ): RecyclerView.ViewHolder(binding.root)

    fun onClicked() = callbackFlow {
        onClickListener = { widget, spanX, spanY ->
            trySend(Triple(widget, spanX, spanY))
        }
        awaitClose {
            onClickListener = null
        }
    }.debounce(TAP_DEBOUNCE)

}