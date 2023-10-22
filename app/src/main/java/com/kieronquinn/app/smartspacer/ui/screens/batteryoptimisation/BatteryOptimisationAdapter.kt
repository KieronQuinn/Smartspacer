package com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation

import android.graphics.Paint
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.databinding.ItemBatteryOptimisationFooterBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItemType
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation.BatteryOptimisationViewModel.BatteryOptimisationSettingsItem
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

class BatteryOptimisationAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<BatteryOptimisationSettingsItem.ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            BatteryOptimisationSettingsItem.ItemType.FOOTER -> BatteryOptimisationViewHolder.Footer(
                ItemBatteryOptimisationFooterBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is BatteryOptimisationViewHolder.Footer -> {
                val item = items[position] as BatteryOptimisationSettingsItem.Footer
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun BatteryOptimisationViewHolder.Footer.setup(
        item: BatteryOptimisationSettingsItem.Footer
    ) = with(binding.batteryOptimisationFooterLink) {
        val accent = monet.getAccentColor(context)
        val primary = monet.getPrimaryColor(context)
        background.setTint(primary)
        setTextColor(accent)
        paintFlags = paintFlags or Paint.ANTI_ALIAS_FLAG or Paint.UNDERLINE_TEXT_FLAG
        whenResumed {
            onClicked().collect {
                item.onLinkClicked()
            }
        }
    }

    sealed class BatteryOptimisationViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Footer(override val binding: ItemBatteryOptimisationFooterBinding):
            BatteryOptimisationViewHolder(binding)
    }

}