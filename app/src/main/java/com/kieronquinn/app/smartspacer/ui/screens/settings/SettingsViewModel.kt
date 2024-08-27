package com.kieronquinn.app.smartspacer.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItemType
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport.Companion.isNativeModeAvailable
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.HideSensitive
import com.kieronquinn.app.smartspacer.utils.extensions.getSupportedLocales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onEnhancedClicked()
    abstract fun onNativeClicked()
    abstract fun onExpandedModeClicked()
    abstract fun onHideSensitiveContentClicked()
    abstract fun onOemSmartspaceClicked()
    abstract fun onNotificationWidgetClicked()
    abstract fun onBackupRestoreClicked()
    abstract fun onPermissionsClicked()
    abstract fun onBatteryOptimisationClicked()
    abstract fun onPluginRepositoryClicked()
    abstract fun onCheckForUpdatesChanged(enabled: Boolean)
    abstract fun onEnableAnalyticsChanged(enabled: Boolean)
    abstract fun onLanguageClicked()
    abstract fun onDebugClicked()

    abstract fun onContributorsClicked()
    abstract fun onDonateClicked()
    abstract fun onGitHubClicked()
    abstract fun onCrowdinClicked()
    abstract fun onLibrariesClicked()
    abstract fun onTwitterClicked()
    abstract fun onXdaClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val enhancedCompatible: Boolean,
            val enhancedEnabled: Boolean,
            val supportsNativeSmartspace: Boolean,
            val hideSensitive: HideSensitive,
            val checkForUpdates: Boolean,
            val enableAnalytics: Boolean,
            val supportedLocales: List<String>
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class SettingsSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class About(
            val onContributorsClicked: () -> Unit,
            val onDonateClicked: () -> Unit,
            val onGitHubClicked: () -> Unit,
            val onCrowdinClicked: () -> Unit,
            val onTwitterClicked: () -> Unit,
            val onXdaClicked: () -> Unit,
            val onLibrariesClicked: () -> Unit
        ): SettingsSettingsItem(ItemType.ABOUT)

        enum class ItemType: BaseSettingsItemType {
            ABOUT
        }
    }

}

class SettingsViewModelImpl(
    context: Context,
    settingsRepository: SmartspacerSettingsRepository,
    private val compatibilityRepository: CompatibilityRepository,
    private val navigation: ContainerNavigation
): SettingsViewModel() {

    companion object {
        private const val LINK_TWITTER = "https://kieronquinn.co.uk/redirect/Smartspacer/twitter"
        private const val LINK_GITHUB = "https://kieronquinn.co.uk/redirect/Smartspacer/github"
        private const val LINK_CROWDIN = "https://kieronquinn.co.uk/redirect/Smartspacer/crowdin"
        private const val LINK_XDA = "https://kieronquinn.co.uk/redirect/Smartspacer/xda"
    }

    private val onResume = MutableStateFlow(System.currentTimeMillis())

    private val enhancedCompatible = flow {
        emit(compatibilityRepository.isEnhancedModeAvailable())
    }

    private val enhancedEnabled = settingsRepository.enhancedMode
    private val checkForUpdates = settingsRepository.updateCheckEnabled
    private val analyticsEnabled = settingsRepository.analyticsEnabled

    private val supportedLocales = flow {
        emit(context.getSupportedLocales().map { it.toLanguageTag() })
    }

    private val enhanced = combine(
        enhancedCompatible,
        enhancedEnabled.asFlow()
    ) { compatible, enabled ->
        Pair(compatible, enabled)
    }

    private val options = combine(
        settingsRepository.hideSensitive.asFlow(),
        checkForUpdates.asFlow(),
        analyticsEnabled.asFlow(),
        supportedLocales
    ) { hideSensitive, checkForUpdates, analyticsEnabled, supportedLocales ->
        Options(hideSensitive, checkForUpdates, analyticsEnabled, supportedLocales)
    }

    override val state = combine(
        enhanced,
        options,
        onResume
    ) { enhanced, options, _ ->
        //If there's no compatible apps at all, native should not be accessible
        val isNativeSmartspaceAvailable =
            compatibilityRepository.getCompatibilityReports().isNativeModeAvailable()
        State.Loaded(
            enhanced.first,
            enhanced.second,
            isNativeSmartspaceAvailable,
            options.hideSensitive,
            options.checkForUpdates,
            options.analyticsEnabled,
            options.supportedLocales
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            onResume.emit(System.currentTimeMillis())
        }
    }

    override fun onEnhancedClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToEnhancedModeFragment2(false))
        }
    }

    override fun onNativeClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToNativeModeFragment2(
                isSetup = false,
                isFromSettings = true
            ))
        }
    }

    override fun onExpandedModeClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToExpandedSettingsFragment(true))
        }
    }

    override fun onHideSensitiveContentClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsHideSensitiveFragment())
        }
    }

    override fun onOemSmartspaceClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsOemSmartspaceFragment())
        }
    }

    override fun onNotificationWidgetClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToNotificationWidgetSettingsFragment())
        }
    }

    override fun onBackupRestoreClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToNavGraphBackupRestore())
        }
    }

    override fun onPermissionsClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToPermissionsFragment())
        }
    }

    override fun onBatteryOptimisationClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsBatteryOptimisationFragment())
        }
    }

    override fun onPluginRepositoryClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToPluginRepositorySettingsFragment())
        }
    }

    override fun onCheckForUpdatesChanged(enabled: Boolean) {
        viewModelScope.launch {
            checkForUpdates.set(enabled)
        }
    }

    override fun onEnableAnalyticsChanged(enabled: Boolean) {
        viewModelScope.launch {
            analyticsEnabled.set(enabled)
        }
    }

    override fun onLanguageClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsLanguageFragment())
        }
    }

    override fun onDebugClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToDumpSmartspacerFragment())
        }
    }

    override fun onContributorsClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToContributorsFragment())
        }
    }

    override fun onDonateClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToNavGraphIncludeDonate())
        }
    }

    override fun onGitHubClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_GITHUB.toIntent())
        }
    }

    override fun onCrowdinClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_CROWDIN.toIntent())
        }
    }

    override fun onLibrariesClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToOssLicensesMenuActivity())
        }
    }

    override fun onTwitterClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_TWITTER.toIntent())
        }
    }

    override fun onXdaClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_XDA.toIntent())
        }
    }

    private fun String.toIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(this@toIntent)
        }
    }

    private data class Options(
        val hideSensitive: HideSensitive,
        val checkForUpdates: Boolean,
        val analyticsEnabled: Boolean,
        val supportedLocales: List<String>
    )

}