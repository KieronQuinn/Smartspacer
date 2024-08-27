package com.kieronquinn.app.smartspacer.ui.screens.settings

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.ItemSettingsAboutBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItemType
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.settings.SettingsViewModel.SettingsSettingsItem.About
import com.kieronquinn.app.smartspacer.ui.screens.settings.SettingsViewModel.SettingsSettingsItem.ItemType
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

class SettingsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    private val chipBackground by lazy {
        ColorStateList.valueOf(monet.getPrimaryColor(recyclerView.context))
    }

    private val googleSansTextMedium by lazy {
        ResourcesCompat.getFont(recyclerView.context, R.font.google_sans_text_medium)
    }

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: BaseSettingsItemType
    ): ViewHolder {
        return when(itemType){
            ItemType.ABOUT -> SettingsViewHolder.About(
                ItemSettingsAboutBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is SettingsViewHolder.About -> {
                val item = items[position] as About
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun SettingsViewHolder.About.setup(about: About) = with(binding) {
        val context = root.context
        val content = context.getString(R.string.about_version, BuildConfig.VERSION_NAME)
        itemUpdatesAboutContent.text = content
        root.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        )
        mapOf(
            itemUpdatesAboutContributors to about.onContributorsClicked,
            itemUpdatesAboutDonate to about.onDonateClicked,
            itemUpdatesAboutGithub to about.onGitHubClicked,
            itemUpdatesAboutCrowdin to about.onCrowdinClicked,
            itemUpdatesAboutLibraries to about.onLibrariesClicked,
            itemUpdatesAboutTwitter to about.onTwitterClicked,
            itemUpdatesAboutXda to about.onXdaClicked
        ).forEach { chip ->
            with(chip.key){
                chipBackgroundColor = chipBackground
                typeface = googleSansTextMedium
                whenResumed {
                    onClicked().collect {
                        chip.value()
                    }
                }
            }
        }
    }

    sealed class SettingsViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class About(override val binding: ItemSettingsAboutBinding):
            SettingsViewHolder(binding)
    }

}