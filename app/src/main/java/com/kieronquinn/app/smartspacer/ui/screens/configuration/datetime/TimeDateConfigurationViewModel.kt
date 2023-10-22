package com.kieronquinn.app.smartspacer.ui.screens.configuration.datetime

import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.TimeDateRequirement.TimeDateRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

abstract class TimeDateConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val errorBus: Flow<Int>

    abstract fun setupWithId(id: String)
    abstract fun onChipClicked(day: DayOfWeek)
    abstract fun onStartTimeSelected(startTime: LocalTime)
    abstract fun onEndTimeSelected(endTime: LocalTime)
    abstract fun onSaveClicked()

    sealed class State {
        object Loading: State()
        object Success: State()
        data class Loaded(
            val id: String,
            var selectedDays: Set<DayOfWeek>,
            var startTime: LocalTime,
            var endTime: LocalTime
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

}

class TimeDateConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    private val gson: Gson,
    scope: CoroutineScope? = null
): TimeDateConfigurationViewModel(scope) {

    private val requirementId = MutableStateFlow<String?>(null)

    override val state = MutableStateFlow<State>(State.Loading)
    override val errorBus = MutableSharedFlow<Int>()

    override fun setupWithId(id: String) {
        vmScope.launch {
            requirementId.emit(id)
        }
    }

    private fun TimeDateRequirementData.toState(): State.Loaded {
        return State.Loaded(id, days, startTime, endTime)
    }

    override fun onStartTimeSelected(startTime: LocalTime) {
        vmScope.launch {
            val currentState = state.value as? State.Loaded ?: return@launch
            if(startTime.isAfter(currentState.endTime)){
                errorBus.emit(R.string.requirement_time_date_configuration_toast_start_time_invalid)
                return@launch
            }
            currentState.startTime = startTime
            state.emit(currentState)
        }
    }

    override fun onEndTimeSelected(endTime: LocalTime) {
        vmScope.launch {
            val currentState = state.value as? State.Loaded ?: return@launch
            if(endTime.isBefore(currentState.startTime)){
                errorBus.emit(R.string.requirement_time_date_configuration_toast_end_time_invalid)
                return@launch
            }
            currentState.endTime = endTime
            state.emit(currentState)
        }
    }

    override fun onChipClicked(day: DayOfWeek) {
        vmScope.launch {
            val currentState = state.value as? State.Loaded ?: return@launch
            if(currentState.selectedDays.contains(day)){
                currentState.selectedDays = currentState.selectedDays - day
            }else{
                currentState.selectedDays = currentState.selectedDays + day
            }
            state.emit(currentState)
        }
    }

    override fun onSaveClicked() {
        val timeDateRequirementData = (state.value as? State.Loaded)?.let {
            TimeDateRequirementData(it.id, it.startTime, it.endTime, it.selectedDays)
        } ?: return
        vmScope.launch {
            if(timeDateRequirementData.days.isEmpty()){
                errorBus.emit(R.string.requirement_time_date_configuration_pick_a_day)
                return@launch
            }
            val requirementData = RequirementData(
                timeDateRequirementData.id,
                RequirementDataType.TIME_DATE.name,
                gson.toJson(timeDateRequirementData)
            )
            dataRepository.addRequirementData(requirementData)
            state.emit(State.Success)
        }
    }

    private fun load() = vmScope.launch {
        requirementId.filterNotNull().collect { id ->
            val requirement = dataRepository.getRequirementData(
                id, TimeDateRequirementData::class.java
            ) ?: TimeDateRequirementData(id)
            state.emit(requirement.toState())
        }
    }

    init {
        load()
    }

}