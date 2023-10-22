package com.kieronquinn.app.smartspacer.ui.screens.native.settings.pagelimit

import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TargetCountLimit
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsViewModelImpl

class NativeModePageLimitViewModel(
    settingsRepository: SmartspacerSettingsRepository
): BaseRadioSettingsViewModelImpl<TargetCountLimit>() {

    override val setting = settingsRepository.nativeTargetCountLimit

}