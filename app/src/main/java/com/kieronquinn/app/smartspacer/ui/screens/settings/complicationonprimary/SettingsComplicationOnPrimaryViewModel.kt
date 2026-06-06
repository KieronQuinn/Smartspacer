package com.kieronquinn.app.smartspacer.ui.screens.settings.complicationonprimary

import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ComplicationOnPrimary
import com.kieronquinn.app.smartspacer.ui.base.settings.radio.BaseRadioSettingsViewModelImpl

class SettingsComplicationOnPrimaryViewModel(
    settingsRepository: SmartspacerSettingsRepository
): BaseRadioSettingsViewModelImpl<ComplicationOnPrimary>() {

    override val setting = settingsRepository.complicationOnPrimary

}