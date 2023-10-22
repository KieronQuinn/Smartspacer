package com.kieronquinn.app.smartspacer.ui.screens.setup.analytics

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import kotlinx.coroutines.launch

abstract class SetupAnalyticsViewModel: ViewModel() {

    abstract fun onLinkClicked(url: String)
    abstract fun onAllowClicked()
    abstract fun onDenyClicked()

    abstract fun onBackPressed(): Boolean

}

class SetupAnalyticsViewModelImpl(
    private val navigation: RootNavigation,
    private val settingsRepository: SmartspacerSettingsRepository
): SetupAnalyticsViewModel() {

    override fun onLinkClicked(url: String) {
        viewModelScope.launch {
            navigation.navigate(url.toIntent())
        }
    }

    override fun onAllowClicked() {
        viewModelScope.launch {
            settingsRepository.analyticsEnabled.set(true)
            navigation.navigate(SetupAnalyticsFragmentDirections.actionSetupAnalyticsFragmentToSetupContainerFragment())
        }
    }

    override fun onDenyClicked() {
        viewModelScope.launch {
            settingsRepository.analyticsEnabled.set(false)
            navigation.navigate(SetupAnalyticsFragmentDirections.actionSetupAnalyticsFragmentToSetupContainerFragment())
        }
    }

    override fun onBackPressed(): Boolean {
        viewModelScope.launch {
            navigation.navigateBack()
        }
        return true
    }

    private fun String.toIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(this@toIntent)
        }
    }

}