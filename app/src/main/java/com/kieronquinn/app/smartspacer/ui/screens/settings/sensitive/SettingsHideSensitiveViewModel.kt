package com.kieronquinn.app.smartspacer.ui.screens.settings.sensitive

import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.HideSensitive
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsViewModelImpl

class SettingsHideSensitiveViewModel(
    settingsRepository: SmartspacerSettingsRepository
): BaseRadioSettingsViewModelImpl<HideSensitive>() {

    override val setting = settingsRepository.hideSensitive

}