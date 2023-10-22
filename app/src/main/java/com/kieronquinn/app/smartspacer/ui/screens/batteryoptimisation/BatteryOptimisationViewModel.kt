package com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.kieronquinn.app.smartspacer.components.navigation.BaseNavigation
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItemType
import com.kieronquinn.app.smartspacer.repositories.BatteryOptimisationRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BatteryOptimisationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun reload()

    abstract fun onBatteryOptimisationClicked()
    abstract fun onOemBatteryOptimisationClicked()
    abstract fun onLearnMoreClicked()
    abstract fun moveToNext()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val batteryOptimisationsDisabled: Boolean,
            val oemBatteryOptimisationAvailable: Boolean
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class BatteryOptimisationSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Footer(val onLinkClicked: () -> Unit):
            BatteryOptimisationSettingsItem(ItemType.FOOTER)

        enum class ItemType: BaseSettingsItemType {
            FOOTER
        }
    }

}

abstract class BatteryOptimisationViewModelImpl(
    context: Context,
    private val batteryOptimisationRepository: BatteryOptimisationRepository,
    private val navigation: BaseNavigation,
    scope: CoroutineScope? = null
): BatteryOptimisationViewModel(scope) {

    companion object {
        private const val DONT_KILL_ROOT = "https://dontkillmyapp.com/"
        private val DONT_KILL_MAPPING = mapOf(
            Pair("oneplus", "https://dontkillmyapp.com/oneplus"),
            Pair("huawei", "https://dontkillmyapp.com/huawei"),
            Pair("samsung", "https://dontkillmyapp.com/samsung"),
            Pair("xiaomi", "https://dontkillmyapp.com/xiaomi"),
            Pair("meizu", "https://dontkillmyapp.com/meizu"),
            Pair("asus", "https://dontkillmyapp.com/asus"),
            Pair("wiko", "https://dontkillmyapp.com/wiko"),
            Pair("lenovo", "https://dontkillmyapp.com/lenovo"),
            Pair("oppo", "https://dontkillmyapp.com/oppo"),
            Pair("vivo", "https://dontkillmyapp.com/vivo"),
            Pair("realme", "https://dontkillmyapp.com/realme"),
            Pair("blackview", "https://dontkillmyapp.com/blackview"),
            Pair("unihertz", "https://dontkillmyapp.com/unihertz"),
            Pair("nokia", "https://dontkillmyapp.com/hmd-global"),
            Pair("sony", "https://dontkillmyapp.com/sony")
        )
    }

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    override val state = reloadBus.mapLatest {
        State.Loaded(
            batteryOptimisationRepository
                .getDisableBatteryOptimisationsIntent() == null,
            batteryOptimisationRepository.areOemOptimisationsAvailable(context)
        )
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun reload() {
        vmScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    override fun onBatteryOptimisationClicked() {
        vmScope.launch {
            val intent = batteryOptimisationRepository.getDisableBatteryOptimisationsIntent()
                ?: return@launch
            navigation.navigate(intent)
        }
    }

    override fun onOemBatteryOptimisationClicked() {
        vmScope.launch {
            navigation.navigateWithContext {
                batteryOptimisationRepository.startOemOptimisationSettings(it)
            }
        }
    }

    override fun onLearnMoreClicked() {
        vmScope.launch {
            val url = DONT_KILL_MAPPING[Build.MANUFACTURER.lowercase()] ?: DONT_KILL_ROOT
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }
    }

    override fun moveToNext() {
        //No-op by default
    }

}
