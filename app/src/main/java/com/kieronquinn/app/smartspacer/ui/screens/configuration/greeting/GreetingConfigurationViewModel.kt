package com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.GreetingTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.GreetingTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class GreetingConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setupWithId(id: String)
    abstract fun setHideIfNoComplications(enabled: Boolean)
    abstract fun setHideTitleOnAod(enabled: Boolean)
    abstract fun setOpenExpandedOnClick(enabled: Boolean)
    abstract fun onNameClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val name: String,
            val hideIfNoComplications: Boolean,
            val hideTitleOnAod: Boolean,
            val openExpandedOnClick: Boolean
        ): State()
    }

}

class GreetingConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    private val settingsRepository: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): GreetingConfigurationViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)

    private val settings = id.filterNotNull().flatMapLatest { id ->
        dataRepository.getTargetDataFlow(id, TargetData::class.java).map {
            it ?: TargetData(settingsRepository.userName.get())
        }
    }.flowOn(Dispatchers.IO)

    override val state = settings.mapLatest {
        State.Loaded(it.name, it.hideIfNoComplications, it.hideTitleOnAod, it.openExpandedOnClick)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithId(id: String) {
        vmScope.launch {
            this@GreetingConfigurationViewModelImpl.id.emit(id)
        }
    }

    override fun setHideIfNoComplications(enabled: Boolean) {
        updateData(hideIfNoComplications = enabled)
    }

    override fun setHideTitleOnAod(enabled: Boolean) {
        updateData(hideTitleOnAod = enabled)
    }

    override fun setOpenExpandedOnClick(enabled: Boolean) {
        updateData(openExpandedOnClick = enabled)
    }

    override fun onNameClicked() {
        vmScope.launch {
            val smartspacerId = id.value ?: return@launch
            navigation.navigate(GreetingConfigurationFragmentDirections
                .actionGreetingConfigurationFragmentToGreetingConfigurationNameBottomSheetFragment(smartspacerId))
        }
    }

    private fun updateData(
        hideIfNoComplications: Boolean? = null,
        hideTitleOnAod: Boolean? = null,
        openExpandedOnClick: Boolean? = null
    ) {
        val id = id.value ?: return
        vmScope.launch {
            dataRepository.updateTargetData(
                id,
                TargetData::class.java,
                TargetDataType.GREETING,
                ::notifyChange
            ) {
                (it ?: TargetData(settingsRepository.userName.getSync())).let { target ->
                    target.copy(
                        hideIfNoComplications = hideIfNoComplications
                            ?: target.hideIfNoComplications,
                        hideTitleOnAod = hideTitleOnAod ?: target.hideTitleOnAod,
                        openExpandedOnClick = openExpandedOnClick ?: target.openExpandedOnClick
                    )
                }
            }
        }
    }

    private fun notifyChange(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, GreetingTarget::class.java, smartspacerId)
    }

}