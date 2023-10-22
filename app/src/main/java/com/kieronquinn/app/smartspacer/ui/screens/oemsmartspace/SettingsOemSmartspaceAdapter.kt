package com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsSwitchItemBinding
import com.kieronquinn.app.smartspacer.model.glide.PackageIcon
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItemType
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace.SettingsOemSmartspaceViewModel.SettingsOemSmartspaceSettingsItem.App
import com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace.SettingsOemSmartspaceViewModel.SettingsOemSmartspaceSettingsItem.ItemType
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet

class SettingsOemSmartspaceAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    private val glide = Glide.with(recyclerView.context)

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): BaseSettingsAdapter.ViewHolder {
        return when(itemType){
            ItemType.APP -> ViewHolder(
                ItemSettingsSwitchItemBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: BaseSettingsAdapter.ViewHolder, position: Int) {
        when(holder){
            is ViewHolder -> {
                val item = items[position] as App
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun ViewHolder.setup(app: App) = with(binding) {
        val showSubtitle = app.app.duplicateAppName || app.subtitle != null
        itemSettingsSwitchTitle.text = app.app.appName
        itemSettingsSwitchContent.text = app.subtitle ?: app.app.packageName
        itemSettingsSwitchContent.isVisible = showSubtitle
        itemSettingsSwitchIcon.imageTintList = null
        glide.load(PackageIcon(app.app.packageName))
            .placeholder(itemSettingsSwitchIcon.drawable)
            .into(itemSettingsSwitchIcon)
        itemSettingsSwitchSwitch.isChecked = app.app.enabled
        itemSettingsSwitchSwitch.applyMonet()
        whenResumed {
            binding.itemSettingsSwitchSwitch.onChanged().collect {
                app.onChanged(it)
            }
        }
        binding.root.setOnClickListener {
            binding.itemSettingsSwitchSwitch.toggle()
        }
    }

    data class ViewHolder(override val binding: ItemSettingsSwitchItemBinding):
        BaseSettingsAdapter.ViewHolder(binding)

}