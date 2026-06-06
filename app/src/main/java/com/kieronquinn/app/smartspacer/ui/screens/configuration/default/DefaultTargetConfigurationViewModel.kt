package com.kieronquinn.app.smartspacer.ui.screens.configuration.default

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget.TargetData
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget.TargetType
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class DefaultTargetConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun setupWithId(smartspacerId: String)
    abstract fun onAtAGlanceClicked()
    abstract fun onHiddenTargetChanged(target: HiddenTarget, enabled: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val showRemoteViewsWarning: Boolean,
            val settings: List<HiddenTarget>
        ): State()
    }

    data class HiddenTarget(val type: TargetType, val isEnabled: Boolean)

}

class DefaultTargetConfigurationViewModelImpl(
    private val navigation: ConfigurationNavigation,
    private val dataRepository: DataRepository,
    compatibilityRepository: CompatibilityRepository,
    context: Context,
    scope: CoroutineScope? = null
): DefaultTargetConfigurationViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)
    private val configurationIntent = TrampolineActivity.createAsiTrampolineIntent(context)
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val data = id.filterNotNull().flatMapLatest {
        dataRepository.getTargetDataFlow(it, TargetData::class.java).map { data ->
            data ?: TargetData()
        }
    }

    private val showRemoteViewsWarning = resumeBus.mapLatest {
        compatibilityRepository.areGlanceRemoteViewsEnabledButDisabled()
    }

    override val state = combine(
        data, showRemoteViewsWarning
    ) { data, showRemoteViewsWarning ->
        State.Loaded(showRemoteViewsWarning, data.getHiddenTargets())
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    private fun TargetData.getHiddenTargets(): List<HiddenTarget> {
        return TargetType.entries.map {
            HiddenTarget(it, hiddenTargetTypes.contains(it.type))
        }
    }

    override fun onResume() {
        vmScope.launch {
            resumeBus.emit(System.currentTimeMillis())
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