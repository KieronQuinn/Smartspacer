package com.kieronquinn.app.smartspacer.ui.screens.setup.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.hasNotificationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupNotificationsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onBackPressed(): Boolean
    abstract fun checkPermission(context: Context)
    abstract fun onPermissionResult(context: Context, granted: Boolean)
    abstract fun onGrantClicked(launcher: ActivityResultLauncher<String>)

}

@SuppressLint("InlinedApi")
class SetupNotificationsViewModelImpl(
    private val navigation: SetupNavigation,
    private val rootNavigation: RootNavigation,
    private val compatibilityRepository: CompatibilityRepository,
    scope: CoroutineScope? = null
): SetupNotificationsViewModel(scope) {

    override fun checkPermission(context: Context) {
        if(context.hasNotificationPermission()){
            vmScope.launch {
                val shouldShowEnhanced = compatibilityRepository.isEnhancedModeAvailable()
                if(shouldShowEnhanced) {
                    navigation.navigate(
                        SetupNotificationsFragmentDirections.actionSetupNotificationsFragmentToEnhancedModeFragment(true)
                    )
                }else{
                    navigation.navigate(
                        SetupNotificationsFragmentDirections.actionSetupNotificationsFragmentToSetupTargetsFragment()
                    )
                }
            }
        }
    }

    override fun onPermissionResult(context: Context, granted: Boolean) {
        if(granted){
            checkPermission(context)
        }else{
            vmScope.launch {
                navigation.navigate(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                })
            }
        }
    }

    override fun onGrantClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onBackPressed(): Boolean {
        vmScope.launch {
            rootNavigation.navigateBack()
        }
        return true
    }

}