package com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemProperties
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.Smartspacer.Companion.PACKAGE_KEYGUARD
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItemType
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsOemSmartspaceViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onAppChanged(packageName: String, enabled: Boolean)
    abstract fun onHideIncompatibleChanged(enabled: Boolean)
    abstract fun onIncompatibleClicked()
    abstract fun onReadMoreClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val compatible: Boolean,
            val apps: List<GrantApp>,
            val hideIncompatible: Boolean
        ): State()
    }

    data class GrantApp(
        val packageName: String,
        val appName: CharSequence,
        val duplicateAppName: Boolean,
        val enabled: Boolean,
        val mayRequireAdditionalSetup: Boolean
    )

    sealed class SettingsOemSmartspaceSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class App(
            val app: GrantApp,
            val subtitle: CharSequence? = null,
            val onChanged: (Boolean) -> Unit
        ): SettingsOemSmartspaceSettingsItem(ItemType.APP)

        enum class ItemType: BaseSettingsItemType {
            APP
        }
    }

}

class SettingsOemSmartspaceViewModelImpl(
    private val navigation: ContainerNavigation,
    private val grantRepository: GrantRepository,
    oemSmartspacerRepository: OemSmartspacerRepository,
    context: Context,
    settingsRepository: SmartspacerSettingsRepository,
    shizukuServiceRepository: ShizukuServiceRepository,
    scope: CoroutineScope? = null
): SettingsOemSmartspaceViewModel(scope) {

    private val packageManager = context.packageManager
    private val enabled = settingsRepository.oemSmartspaceEnabled
    private val hideIncompatible = settingsRepository.oemHideIncompatible
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val systemUiLabel = context.getString(R.string.oem_smartspace_label_systemui)

    private val apps = combine(
        oemSmartspacerRepository.getCompatibleApps(),
        grantRepository.grants.filterNotNull()
    ) { apps, grants ->
        val grantedPackages = grants.filter { it.oemSmartspace }.map { it.packageName }
        val appNames = mutableSetOf<CharSequence>()
        apps.map {
            val packageName = it.packageName
            val label = if(packageName == PACKAGE_KEYGUARD){
                systemUiLabel
            }else{
                it.loadLabel(packageManager)
            }
            val duplicate = appNames.contains(label)
            appNames.add(label)
            GrantApp(
                packageName,
                label,
                duplicate,
                grantedPackages.contains(packageName),
                packageName.willRequireAdditionalSetup()
            )
        }.sortedBy {
            it.appName.toString().lowercase()
        }
    }.flowOn(Dispatchers.IO)

    private val isCompatible = resumeBus.mapLatest {
        shizukuServiceRepository.runWithSuiService {
            it.isCompatible
        }.unwrap() ?: false
    }

    override val state = combine(
        isCompatible,
        apps,
        enabled.asFlow(),
        hideIncompatible.asFlow()
    ) { compatible, available, isEnabled, hide ->
        State.Loaded(isEnabled, compatible, available, hide)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onEnabledChanged(enabled: Boolean) {
        vmScope.launch {
            this@SettingsOemSmartspaceViewModelImpl.enabled.set(enabled)
        }
    }

    override fun onHideIncompatibleChanged(enabled: Boolean) {
        vmScope.launch {
            hideIncompatible.set(enabled)
        }
    }

    override fun onAppChanged(packageName: String, enabled: Boolean) {
        vmScope.launch {
            val grant = grantRepository.getGrantForPackage(packageName) ?: Grant(packageName)
            grant.oemSmartspace = enabled
            grantRepository.addGrant(grant)
        }
    }

    override fun onIncompatibleClicked() {
        vmScope.launch {
            val suiIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/RikkaApps/Sui")
            }
            navigation.navigate(suiIntent)
        }
    }

    override fun onReadMoreClicked() {
        vmScope.launch {
            val oemSmartspaceIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://kieronquinn.co.uk/redirect/Smartspacer/oemsmartspace")
            }
            navigation.navigate(oemSmartspaceIntent)
        }
    }

    private fun String.willRequireAdditionalSetup(): Boolean {
        return when(this) {
            PACKAGE_KEYGUARD -> willSystemUIRequireAdditionalSetup()
            else -> false
        }
    }

    private fun willSystemUIRequireAdditionalSetup(): Boolean {
        //Oxygen/ColorOS, requires an extra property set for 12+
        if(SystemProperties.get("ro.boot.project_codename", "").isNotEmpty()){
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }
        return false
    }

}