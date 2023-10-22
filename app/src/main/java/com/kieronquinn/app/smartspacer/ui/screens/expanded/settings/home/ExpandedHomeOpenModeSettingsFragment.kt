package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.home

import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ExpandedHomeOpenModeSettingsFragment: BaseRadioSettingsFragment<ExpandedOpenMode>(), BackAvailable, HideBottomNavigation {

    private val args by navArgs<ExpandedHomeOpenModeSettingsFragmentArgs>()

    override val viewModel by viewModel<ExpandedHomeOpenModeSettingsViewModel>()

    override fun getSettingTitle(setting: ExpandedOpenMode): CharSequence {
        return getString(setting.label)
    }

    override fun getSettingContent(setting: ExpandedOpenMode): CharSequence {
        return getString(setting.content)
    }

    override fun getValues(): List<ExpandedOpenMode> {
        return ExpandedOpenMode.values().toList()
    }

    override fun shouldHideBottomNavigation(): Boolean {
        return !args.isFromSettings
    }

}