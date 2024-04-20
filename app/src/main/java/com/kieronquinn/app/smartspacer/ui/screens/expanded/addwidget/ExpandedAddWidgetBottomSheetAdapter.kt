package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemWidgetAddAppBinding
import com.kieronquinn.app.smartspacer.databinding.ItemWidgetAddPredictedBinding
import com.kieronquinn.app.smartspacer.databinding.ItemWidgetAddWidgetBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.getColorSurface
import com.kieronquinn.app.smartspacer.utils.extensions.getHeightSpan
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetColumnWidth
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetRowHeight
import com.kieronquinn.app.smartspacer.utils.extensions.getWidthSpan
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.onPageChanged
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.app.smartspacer.utils.viewpager.ViewPager2ViewHeightAnimator
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.R as MonetcompatR

class ExpandedAddWidgetBottomSheetAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    private val getAvailableWidth: () -> Int,
    private val onExpandClicked: (Item.App) -> Unit,
    private val onWidgetClicked: (Item.Widget, Int, Int) -> Unit
) : LifecycleAwareRecyclerView.ListAdapter<Item, ViewHolder>(createDiffUtil(), recyclerView), BaseExpandedAddWidgetBottomSheetAdapter {

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

    private val monet by lazy {
        MonetCompat.getInstance()
    }

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
            Item.Type.PREDICTED -> {
                ViewHolder.Predicted(
                    ItemWidgetAddPredictedBinding.inflate(layoutInflater, parent, false)
                )
            }
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
            is ViewHolder.Predicted -> holder.setup(currentList[position] as Item.Predicted)
            is ViewHolder.App -> holder.setup(currentList[position] as Item.App)
            is ViewHolder.Widget -> {
                val isLast = currentList.getOrNull(position + 1) !is Item.Widget
                holder.setup(currentList[position] as Item.Widget, isLast)
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
            widgetAddWidgetImage,
            widgetAddWidgetContainer,
            widgetAddWidgetName,
            widgetAddWidgetDescription
        )
        whenResumed {
            root.onClicked().collect {
                onWidgetClicked(item, spanX, spanY)
            }
        }
    }

    private fun ViewHolder.Predicted.setup(item: Item.Predicted) = with(binding) {
        val setCategory = {
            item.widgets.getOrNull(widgetAddPredictedViewPager.currentItem)?.category?.let {
                widgetAddPredictedCategory.setText(it.labelRes)
            }
        }
        val adapter = ExpandedAddWidgetBottomSheetViewPagerAdapter(
            root.context,
            glide,
            widgetContext,
            getAvailableWidth,
            item.widgets
        )
        widgetAddPredictedViewPager.adapter = adapter
        widgetAddPredictedDots.attachTo(widgetAddPredictedViewPager)
        widgetAddPredictedDots.setStrokeDotsIndicatorColor(monet.getColorSurface(context))
        widgetAddPredictedDots.setDotIndicatorColor(monet.getAccentColor(context))
        setCategory()
        whenResumed {
            ViewPager2ViewHeightAnimator.register(widgetAddPredictedViewPager)
        }
        whenResumed {
            widgetAddPredictedViewPager.onPageChanged().collect {
                setCategory()
            }
        }
        whenResumed {
            adapter.onClicked().collect {
                onWidgetClicked(it.first, it.second, it.third)
            }
        }
    }

    sealed class ViewHolder(
        open val binding: ViewBinding
    ) : LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class Predicted(override val binding: ItemWidgetAddPredictedBinding): ViewHolder(binding)
        data class App(override val binding: ItemWidgetAddAppBinding) : ViewHolder(binding)
        data class Widget(override val binding: ItemWidgetAddWidgetBinding) : ViewHolder(binding)
    }

}