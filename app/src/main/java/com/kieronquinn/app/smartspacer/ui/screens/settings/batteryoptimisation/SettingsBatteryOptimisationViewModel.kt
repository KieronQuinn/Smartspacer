package com.kieronquinn.app.smartspacer.ui.screens.settings.batteryoptimisation

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.BatteryOptimisationRepository
import com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation.BatteryOptimisationViewModelImpl

abstract class SettingsBatteryOptimisationViewModel(
    context: Context,
    batteryOptimisationRepository: BatteryOptimisationRepository,
    navigation: ContainerNavigation
): BatteryOptimisationViewModelImpl(context, batteryOptimisationRepository, navigation)

class SettingsBatteryOptimisationViewModelImpl(
    context: Context,
    batteryOptimisationRepository: BatteryOptimisationRepository,
    navigation: ContainerNavigation
): SettingsBatteryOptimisationViewModel(context, batteryOptimisationRepository, navigation)