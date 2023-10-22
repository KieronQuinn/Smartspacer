package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.home

import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsViewModelImpl

class ExpandedHomeOpenModeSettingsViewModel(
    settingsRepository: SmartspacerSettingsRepository
): BaseRadioSettingsViewModelImpl<ExpandedOpenMode>() {

    override val setting = settingsRepository.expandedOpenModeHome

}