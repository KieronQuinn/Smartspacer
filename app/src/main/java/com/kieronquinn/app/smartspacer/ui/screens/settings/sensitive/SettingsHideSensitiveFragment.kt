package com.kieronquinn.app.smartspacer.ui.screens.settings.sensitive

import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.HideSensitive
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsHideSensitiveFragment: BaseRadioSettingsFragment<HideSensitive>(), BackAvailable {

    override val viewModel by viewModel<SettingsHideSensitiveViewModel>()

    override fun getSettingTitle(setting: HideSensitive): CharSequence {
        return getString(setting.label)
    }

    override fun getSettingContent(setting: HideSensitive): CharSequence {
        return getString(setting.content)
    }

    override fun getValues(): List<HideSensitive> {
        return HideSensitive.values().toList()
    }

}