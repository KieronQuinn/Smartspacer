package com.kieronquinn.app.smartspacer.ui.screens.configuration.default

import android.content.Context
import androidx.annotation.StringRes
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class DefaultTargetConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setupWithId(smartspacerId: String)
    abstract fun onAtAGlanceClicked()
    abstract fun onHiddenTargetChanged(target: HiddenTarget, enabled: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(val settings: List<HiddenTarget>): State()
    }

    data class HiddenTarget(val type: TargetType, val isEnabled: Boolean)

    enum class TargetType(@StringRes val title: Int, val type: String) {
        DOORBELL(R.string.target_default_settings_hide_doorbell, "DOORBELL"),
        PACKAGE(R.string.target_default_settings_hide_package, "PACKAGE_DELIVERY"),
        TIMER(R.string.target_default_settings_hide_timer, "TIMER_STOPWATCH"),
        BEDTIME(R.string.target_default_settings_hide_bedtime, "BEDTIME"),
        FITNESS(R.string.target_default_settings_hide_fitness, "FITNESS"),
        CONNECTED_DEVICES(R.string.target_default_settings_hide_connected_devices, "CONNECTED_DEVICES"),
        FLASHLIGHT(R.string.target_default_settings_hide_flashlight, "FLASHLIGHT"),
        SAFETY_CHECK(R.string.target_default_settings_hide_safety_check, "SAFETY_CHECK"),
        EARTHQUAKE_ALERT(R.string.target_default_settings_hide_earthquake_alert, "EARTHQUAKE"),
        COMMUTE(R.string.target_default_settings_hide_commute, "COMMUTE"),
        TIME_TO_LEAVE(R.string.target_default_settings_hide_time_to_leave, "TIME_TO_LEAVE"),
        WEATHER_ALERTS(R.string.target_default_settings_hide_weather_alerts, "WEATHER_ALERT"),
        TRAVEL(R.string.target_default_settings_hide_travel, "FLIGHT"),
        CALENDAR(R.string.target_default_settings_hide_calendar, "CALENDAR"),
        WORK_PROFILE(R.string.target_default_settings_hide_work_profile, "WORK_PROFILE"),
        FOOD(R.string.target_default_settings_hide_food, "FOOD_DELIVERY_ETA"),
        CROSS_DEVICE_TIMER(R.string.target_default_settings_cross_device_timer, "CROSS_DEVICE_TIMER")
    }

}

class DefaultTargetConfigurationViewModelImpl(
    private val navigation: ConfigurationNavigation,
    private val dataRepository: DataRepository,
    context: Context,
    scope: CoroutineScope? = null
): DefaultTargetConfigurationViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)
    private val configurationIntent = TrampolineActivity.createAsiTrampolineIntent(context)

    private val data = id.filterNotNull().flatMapLatest {
        dataRepository.getTargetDataFlow(it, TargetData::class.java).map { data ->
            data ?: TargetData()
        }
    }

    override val state = data.mapLatest {
        State.Loaded(it.getHiddenTargets())
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    private fun TargetData.getHiddenTargets(): List<HiddenTarget> {
        return TargetType.values().map {
            HiddenTarget(it, hiddenTargetTypes.contains(it.type))
        }
    }

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            id.emit(smartspacerId)
        }
    }

    override fun onAtAGlanceClicked() {
        vmScope.launch {
            navigation.navigate(configurationIntent ?: return@launch)
        }
    }

    override fun onHiddenTargetChanged(target: HiddenTarget, enabled: Boolean) {
        val id = id.value ?: return
        dataRepository.updateTargetData(
            id,
            TargetData::class.java,
            TargetDataType.DEFAULT,
            ::onTargetUpdated
        ) {
            val data = it ?: TargetData()
            if(enabled) {
                data.copy(hiddenTargetTypes = data.hiddenTargetTypes.plus(target.type.type))
            }else{
                data.copy(hiddenTargetTypes = data.hiddenTargetTypes.minus(target.type.type))
            }
        }
    }

    private fun onTargetUpdated(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, DefaultTarget::class.java, smartspacerId)
    }

}