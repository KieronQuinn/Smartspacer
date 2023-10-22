package com.kieronquinn.app.smartspacer.ui.screens.donate

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class DonateViewModel: ViewModel() {

    abstract val isDonatePromptEnabled: StateFlow<Boolean>

    abstract fun onPayPalClicked()
    abstract fun onDisableClicked()

}

class DonateViewModelImpl(
    private val navigation: ContainerNavigation,
    settings: SmartspacerSettingsRepository
): DonateViewModel() {

    companion object {
        private const val LINK_PAYPAL = "https://kieronquinn.co.uk/redirect/Smartspacer/donate/paypal"
    }

    private val donatePromptEnabled = settings.donatePromptEnabled

    override val isDonatePromptEnabled = donatePromptEnabled.asFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, donatePromptEnabled.getSync())

    override fun onPayPalClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_PAYPAL.toIntent())
        }
    }

    override fun onDisableClicked() {
        viewModelScope.launch {
            donatePromptEnabled.set(false)
        }
    }

    private fun String.toIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(this@toIntent)
        }
    }

}