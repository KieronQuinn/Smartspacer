package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.lock

import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsViewModelImpl

class ExpandedLockOpenModeSettingsViewModel(
    settingsRepository: SmartspacerSettingsRepository
): BaseRadioSettingsViewModelImpl<ExpandedOpenMode>() {

    override val setting = settingsRepository.expandedOpenModeLock

}