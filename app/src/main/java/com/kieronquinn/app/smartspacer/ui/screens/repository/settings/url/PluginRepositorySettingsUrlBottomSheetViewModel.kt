package com.kieronquinn.app.smartspacer.ui.screens.repository.settings.url

import android.webkit.URLUtil
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class PluginRepositorySettingsUrlBottomSheetViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val url: String

    abstract fun setUrl(name: String)
    abstract fun onPositiveClicked()
    abstract fun onNegativeClicked()
    abstract fun onNeutralClicked()

}

class PluginRepositorySettingsUrlBottomSheetViewModelImpl(
    private val settings: SmartspacerSettingsRepository,
    private val navigation: ContainerNavigation,
    scope: CoroutineScope? = null
): PluginRepositorySettingsUrlBottomSheetViewModel(scope) {

    private var _url = settings.pluginRepositoryUrl.getSync()

    override val url
        get() = _url

    override fun setUrl(name: String) {
        _url = name
    }

    override fun onPositiveClicked() {
        vmScope.launch {
            if(URLUtil.isValidUrl(_url)) {
                settings.pluginRepositoryUrl.set(_url)
            }
            navigation.navigateBack()
        }
    }

    override fun onNegativeClicked() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onNeutralClicked() {
        vmScope.launch {
            settings.pluginRepositoryUrl.clear()
            navigation.navigateBack()
        }
    }

}