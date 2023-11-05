package com.kieronquinn.app.smartspacer.ui.screens.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class DisplayOverOtherAppsPermissionBottomSheetViewModel: ViewModel() {

    abstract fun onGrantClicked()
    abstract fun onDismissClicked()

}

class DisplayOverOtherAppsPermissionBottomSheetViewModelImpl(
    private val navigation: ContainerNavigation
): DisplayOverOtherAppsPermissionBottomSheetViewModel() {

    override fun onGrantClicked() {
        viewModelScope.launch {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            }
            navigation.navigate(intent)
            delay(250L)
            navigation.navigateBack()
        }
    }

    override fun onDismissClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}