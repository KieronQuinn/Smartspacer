package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.annotation.SuppressLint
import android.util.SizeF
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemWidgetAddAppBinding
import com.kieronquinn.app.smartspacer.databinding.ItemWidgetAddWidgetBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.model.glide.Widget
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.ui.views.appwidget.PreviewAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.extensions.getBestRemoteViews
import com.kieronquinn.app.smartspacer.utils.extensions.getHeightSpan
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetColumnWidth
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetRowHeight
import com.kieronquinn.app.smartspacer.utils.extensions.getWidthSpan
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.loadPreview
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.R as MonetcompatR

class ExpandedAddWidgetBottomSheetAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    private val getAvailableWidth: () -> Int,
    private val onExpandClicked: (Item.App) -> Unit,
    private val onWidgetClicked: (Item.Widget, Int, Int) -> Unit
) : LifecycleAwareRecyclerView.ListAdapter<Item, ViewHolder>(createDiffUtil(), recyclerView) {

    companion object {
        fun createDiffUtil(): DiffUtil.ItemCallback<Item> {
            return object : DiffUtil.ItemCallback<Item>() {
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
                    if (oldItem is Item.App && newItem is Item.App) {
                        return oldItem == newItem
                    }
                    if (oldItem is Item.Widget && newItem is Item.Widget) {
                        return oldItem.info.toString() == newItem.info.toString()
                    }
                    return false
                }

            }
        }
    }

    private val theme = if(recyclerView.context.isDarkMode) {
        MonetcompatR.style.Theme_MaterialComponents
    } else {
        MonetcompatR.style.Theme_MaterialComponents_Light
    }

    private val context = recyclerView.context
    private val widgetContext = ContextThemeWrapper(recyclerView.context.applicationContext, theme)

    private val layoutInflater = LayoutInflater.from(context)
    private val glide = Glide.with(recyclerView.context)

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

    override fun getItemViewType(position: Int): Int {
        return currentList[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (Item.Type.entries[viewType]) {
            Item.Type.APP -> {
                ViewHolder.App(
                    ItemWidgetAddAppBinding.inflate(layoutInflater, parent, false)
                )
            }
            Item.Type.WIDGET -> {
                ViewHolder.Widget(
                    ItemWidgetAddWidgetBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.App -> holder.setup(currentList[position] as Item.App)
            is ViewHolder.Widget -> {
                val isLast = currentList.getOrNull(position + 1) !is Item.Widget
                holder.setup(currentList[position] as Item.Widget, isLast)
            }
            else -> {
                //No-op
            }
        }
    }

    private fun ViewHolder.App.setup(item: Item.App) = with(binding) {
        glide.load(item.icon ?: PackageIcon(item.packageName))
            .placeholder(widgetAddAppIcon.drawable)
            .into(widgetAddAppIcon)
        widgetAddAppName.text = item.label
        val content = root.resources.getQuantityString(
            R.plurals.expanded_add_widget_widgets, item.count, item.count
        )
        widgetAddWidgetCount.text = content
        widgetAddAppArrow.rotation = if (item.isExpanded) 180f else 0f
        root.shapeAppearanceModel = if(item.isExpanded){
            topRoundedShapeAppearance
        }else allRoundedShapeAppearance
        whenResumed {
            root.onClicked().collect {
                onExpandClicked(item)
            }
        }
        whenResumed {
            widgetAddAppArrow.onClicked().collect {
                onExpandClicked(item)
            }
        }
    }

    @SuppressLint("BlockedPrivateApi")
    private fun ViewHolder.Widget.setup(item: Item.Widget, isLast: Boolean) = with(binding) {
        root.shapeAppearanceModel = if(isLast){
            bottomRoundedShapeAppearance
        }else noneRoundedShapeAppearance
        val context = root.context
        val availableWidth = getAvailableWidth()
        val columnWidth = context.getWidgetColumnWidth(availableWidth)
        val rowHeight = context.getWidgetRowHeight(availableWidth)
        val spanX = item.info.getWidthSpan(columnWidth)
        val spanY = item.info.getHeightSpan(rowHeight)
        val spanWidth = columnWidth * spanX
        val spanHeight = rowHeight * spanY
        val previewView = item.info.loadPreview()?.getBestRemoteViews(
            context,
            SizeF(spanWidth.toFloat(), spanHeight.toFloat())
        )
        if (previewView != null) {
            widgetAddWidgetImage.isVisible = false
            widgetAddWidgetContainer.isVisible = true
            widgetAddWidgetContainer.updateLayoutParams<LinearLayout.LayoutParams> {
                height = spanHeight
            }
            val appWidgetHostView = PreviewAppWidgetHostView(widgetContext)
            appWidgetHostView.setAppWidget(
                item.info,
                previewView,
                spanWidth,
                spanHeight
            )
            appWidgetHostView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                spanHeight
            )
            widgetAddWidgetContainer.removeAllViews()
            widgetAddWidgetContainer.addView(appWidgetHostView)
        } else {
            widgetAddWidgetImage.isVisible = true
            widgetAddWidgetContainer.isVisible = false
            widgetAddWidgetImage.updateLayoutParams<LinearLayout.LayoutParams> {
                width = spanWidth
                height = spanHeight
            }
            glide.load(Widget(item.info, spanWidth, spanHeight))
                .into(widgetAddWidgetImage)
                .waitForLayout()
        }
        widgetAddWidgetName.text = root.context.getString(
            R.string.expanded_add_widget_widget_label,
            item.label,
            spanX,
            spanY
        )
        widgetAddWidgetDescription.text = item.description
        widgetAddWidgetDescription.isVisible = item.description != null
        root.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        whenResumed {
            root.onClicked().collect {
                onWidgetClicked(item, spanX, spanY)
            }
        }
    }

    sealed class ViewHolder(
        open val binding: ViewBinding
    ) : LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class App(override val binding: ItemWidgetAddAppBinding) : ViewHolder(binding)
        data class Widget(override val binding: ItemWidgetAddWidgetBinding) : ViewHolder(binding)
    }

}