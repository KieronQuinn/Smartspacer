package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsCardBinding
import com.kieronquinn.app.smartspacer.databinding.ItemWidgetAddAppBinding
import com.kieronquinn.app.smartspacer.databinding.ItemWidgetAddWidgetBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetAdapter.ViewHolder
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.Item
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.awaitPost
import com.kieronquinn.app.smartspacer.utils.extensions.getHeight
import com.kieronquinn.app.smartspacer.utils.extensions.getWidth
import com.kieronquinn.app.smartspacer.utils.extensions.loadPreview
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

class ExpandedAddWidgetBottomSheetAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    private val onExpandClicked: (Item.App) -> Unit,
    private val onWidgetClicked: (Item.Widget) -> Unit
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

    private val layoutInflater = LayoutInflater.from(recyclerView.context)
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
        return when (Item.Type.values()[viewType]) {
            Item.Type.HEADER -> {
                ViewHolder.Header(
                    ItemSettingsCardBinding.inflate(layoutInflater, parent, false)
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
            is ViewHolder.Header -> holder.setup()
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

    private fun ViewHolder.Header.setup() = with(binding) {
        itemSettingsCardIcon.setImageResource(R.drawable.ic_warning)
        itemSettingsCardContent.setText(R.string.expanded_add_widget_warning)
    }

    private fun ViewHolder.App.setup(item: Item.App) = with(binding) {
        glide.load(PackageIcon(item.packageName))
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
        whenResumed {
            root.awaitPost()
            val scaledWidth = (widgetAddWidgetRoot.width / 5f) * item.spanX
            val verticalScale = scaledWidth / item.info.getWidth().toFloat()
            val scaledHeight = item.info.getHeight() * verticalScale
            val previewView = item.info.loadPreview(root.context, widgetAddWidgetContainer)
            if (previewView != null) {
                widgetAddWidgetContainer.removeAllViews()
                widgetAddWidgetContainer.isVisible = true
                widgetAddWidgetImage.isVisible = false
                widgetAddWidgetContainer.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = scaledWidth.toInt()
                    height = scaledHeight.toInt()
                }
                widgetAddWidgetContainer.addView(previewView)
            } else {
                widgetAddWidgetImage.isVisible = true
                widgetAddWidgetContainer.isVisible = false
                widgetAddWidgetImage.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = scaledWidth.toInt()
                    height = scaledHeight.toInt()
                }
                glide.load(item.info)
                    .placeholder(widgetAddWidgetImage.drawable)
                    .fitCenter().into(widgetAddWidgetImage)
            }
        }
        widgetAddWidgetName.text = root.context.getString(
            R.string.expanded_add_widget_widget_label,
            item.label,
            item.spanX,
            item.spanY
        )
        whenResumed {
            root.onClicked().collect {
                onWidgetClicked(item)
            }
        }
    }

    sealed class ViewHolder(
        open val binding: ViewBinding
    ) : LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class Header(override val binding: ItemSettingsCardBinding): ViewHolder(binding)
        data class App(override val binding: ItemWidgetAddAppBinding) : ViewHolder(binding)
        data class Widget(override val binding: ItemWidgetAddWidgetBinding) : ViewHolder(binding)
    }

}