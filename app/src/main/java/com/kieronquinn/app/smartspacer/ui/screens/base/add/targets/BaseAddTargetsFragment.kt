package com.kieronquinn.app.smartspacer.ui.screens.base.add.targets

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.activities.permission.notification.NotificationPermissionActivity
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.permission.NotificationPermissionDialogFragment
import com.kieronquinn.app.smartspacer.ui.screens.permission.WidgetPermissionDialogFragment
import com.kieronquinn.app.smartspacer.utils.extensions.allowBackground
import com.kieronquinn.app.smartspacer.utils.extensions.setClassLoaderToPackage
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import kotlinx.coroutines.flow.drop

abstract class BaseAddTargetsFragment<V: ViewBinding>(inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): BoundFragment<V>(inflate) {

    abstract val viewModel: BaseAddTargetsViewModel

    private val configurationResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onTargetConfigureResult(it.resultCode == Activity.RESULT_OK)
    }

    private val widgetBindResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onWidgetBindResult(it.resultCode == Activity.RESULT_OK)
    }

    private val widgetConfigureResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.onWidgetConfigureResult(it.resultCode == Activity.RESULT_OK)
    }

    private val notificationListenerResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onNotificationListenerGrantResult(it.resultCode == Activity.RESULT_OK)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAddState()
        setupWidgetPermissionDialogResult()
        setupNotificationPermissionDialogResult()
    }

    private fun setupAddState() = whenCreated {
        viewModel.addState.drop(1).collect {
            handleAddState(it)
        }
    }

    @SuppressLint("NewApi")
    private fun handleAddState(state: AddState) {
        when(state) {
            is AddState.ConfigureTarget -> {
                val setupIntent = state.target.setupIntent?.apply {
                    setClassLoaderToPackage(requireContext(), state.target.packageName)
                    putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, state.target.id)
                    putExtra(SmartspacerConstants.EXTRA_AUTHORITY, state.target.authority)
                }
                try {
                    configurationResult.launch(setupIntent)
                }catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        R.string.targets_add_failed_to_launch_setup,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            is AddState.GrantWidgetPermission -> {
                viewModel.showWidgetPermissionsDialog(state.grant)
            }
            is AddState.GrantNotificationPermission -> {
                viewModel.showNotificationPermissionsDialog(state.grant)
            }
            is AddState.GrantNotificationListener -> {
                val notificationPermissionActivity = Intent(
                    requireContext(), NotificationPermissionActivity::class.java
                )
                notificationListenerResult.launch(notificationPermissionActivity)
            }
            is AddState.BindWidget -> {
                if(viewModel.bindAppWidgetIfAllowed(state.info.provider, state.id)){
                    viewModel.onWidgetBindResult(true)
                }else{
                    val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, state.id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, state.info.provider)
                    }
                    widgetBindResult.launch(bindIntent)
                }
            }
            is AddState.ConfigureWidget -> {
                val configureIntentSender = viewModel.createConfigIntentSender(state.id)
                widgetConfigureResult.launch(
                    IntentSenderRequest.Builder(configureIntentSender).build(),
                    ActivityOptionsCompat.makeBasic().allowBackground()
                )
            }
            is AddState.WidgetError -> {
                Toast.makeText(
                    requireContext(), R.string.targets_add_incompatible_widget, Toast.LENGTH_LONG
                ).show()
            }
            is AddState.Dismiss -> {
                onDismiss(state.target)
            }
            is AddState.Idle -> {
                //No-op
            }
        }
    }

    private fun setupWidgetPermissionDialogResult() {
        setFragmentResultListener(WidgetPermissionDialogFragment.REQUEST_WIDGET_PERMISSION) { _, result ->
            val granted = result.getBoolean(WidgetPermissionDialogFragment.RESULT_GRANTED)
            viewModel.onWidgetGrantResult(granted)
        }
    }

    private fun setupNotificationPermissionDialogResult() {
        setFragmentResultListener(NotificationPermissionDialogFragment.REQUEST_NOTIFICATION_PERMISSION) { _, result ->
            val granted = result.getBoolean(NotificationPermissionDialogFragment.RESULT_GRANTED)
            viewModel.onNotificationGrantResult(granted)
        }
    }

    abstract fun onDismiss(target: Item.Target)

}