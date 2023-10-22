package com.kieronquinn.app.smartspacer.ui.screens.configuration.calendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget.TargetData
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget.TargetData.PreEventTime
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.Calendar
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class CalendarTargetConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun reload()
    abstract fun requestPermission(launcher: ActivityResultLauncher<String>)
    abstract fun onPermissionResult(context: Context, granted: Boolean)
    abstract fun setupWithId(smartspacerId: String)
    abstract fun onCalendarChanged(id: String, enabled: Boolean)
    abstract fun onShowAllDayChanged(enabled: Boolean)
    abstract fun onShowLocationChanged(enabled: Boolean)
    abstract fun onShowUnconfirmedChanged(enabled: Boolean)
    abstract fun onUseAlternativeIdsChanged(enabled: Boolean)
    abstract fun onPreEventTimeChanged(time: PreEventTime)
    abstract fun onClearDismissEventsClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val calendars: List<Calendar>,
            val targetData: TargetData
        ): State()
    }

}

class CalendarTargetConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    private val calendarRepository: CalendarRepository,
    private val navigation: ConfigurationNavigation,
    scope: CoroutineScope? = null
): CalendarTargetConfigurationViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)

    @VisibleForTesting
    var hasRequestedPermission = false

    private val targetData = id.filterNotNull().flatMapLatest {
        dataRepository.getTargetDataFlow(it, TargetData::class.java).mapLatest { data ->
            data ?: TargetData(it)
        }
    }

    override val state = combine(
        targetData, calendarRepository.getCalendars()
    ) { data, calendars ->
        State.Loaded(calendars.sortedBy { it.name.lowercase() }, data)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            id.emit(smartspacerId)
        }
    }

    override fun reload() {
        vmScope.launch {
            calendarRepository.checkPermission()
            if(hasRequestedPermission && !calendarRepository.hasPermission()){
                navigation.finish()
            }
        }
    }

    override fun requestPermission(launcher: ActivityResultLauncher<String>) {
        launcher.launch(Manifest.permission.READ_CALENDAR)
    }

    override fun onPermissionResult(context: Context, granted: Boolean) {
        when {
            granted -> {
                calendarRepository.checkPermission()
            }
            !hasRequestedPermission -> {
                hasRequestedPermission = true
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                }
                Toast.makeText(
                    context, R.string.target_calendar_settings_permission_toast, Toast.LENGTH_LONG
                ).show()
                vmScope.launch {
                    navigation.navigate(intent)
                }
            }
            else -> {
                vmScope.launch {
                    navigation.finish()
                }
            }
        }
    }

    override fun onCalendarChanged(id: String, enabled: Boolean) {
        updateTargetData {
            if(it.calendars.contains(id)){
                it.copy(calendars = it.calendars.minus(id))
            }else{
                it.copy(calendars = it.calendars.plus(id))
            }
        }
    }

    override fun onShowAllDayChanged(enabled: Boolean) {
        updateTargetData {
            it.copy(showAllDay = enabled)
        }
    }

    override fun onShowLocationChanged(enabled: Boolean) {
        updateTargetData {
            it.copy(showLocation = enabled)
        }
    }

    override fun onShowUnconfirmedChanged(enabled: Boolean) {
        updateTargetData {
            it.copy(showUnconfirmed = enabled)
        }
    }

    override fun onUseAlternativeIdsChanged(enabled: Boolean) {
        updateTargetData {
            it.copy(useAlternativeEventIds = enabled)
        }
    }

    override fun onPreEventTimeChanged(time: PreEventTime) {
        updateTargetData {
            it.copy(preEventTime = time)
        }
    }

    override fun onClearDismissEventsClicked() {
        updateTargetData {
            it.copy(dismissedEvents = emptySet())
        }
    }

    private fun updateTargetData(block: (TargetData) -> TargetData) {
        val id = id.value ?: return
        dataRepository.updateTargetData(
            id,
            TargetData::class.java,
            TargetDataType.CALENDAR,
            ::onTargetDataChanged
        ) {
            val data = it ?: TargetData(id)
            block(data)
        }
    }

    private fun onTargetDataChanged(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, CalendarTarget::class.java, smartspacerId)
    }

}