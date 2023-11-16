package com.kieronquinn.app.smartspacer.ui.activities

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.WidgetOptionsNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.annotations.LimitedNativeSupport
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import com.kieronquinn.app.smartspacer.ui.screens.widget.SmartspacerWidgetConfigurationFragment
import kotlinx.coroutines.launch

abstract class WidgetOptionsMenuViewModel: ViewModel() {

    abstract fun onDismissClicked(targetId: String)
    abstract fun onAboutClicked(aboutIntent: Intent, errorCallback: () -> Unit)
    abstract fun onFeedbackClicked(feedbackIntent: Intent, errorCallback: () -> Unit)
    abstract fun onSettingsClicked(context: Context)
    abstract fun onConfigureClicked(context: Context, appWidgetId: Int, owner: String)

}

@OptIn(LimitedNativeSupport::class)
class WidgetOptionsMenuViewModelImpl(
    private val navigation: WidgetOptionsNavigation,
    private val smartspaceRepository: SmartspaceRepository
): WidgetOptionsMenuViewModel() {

    override fun onDismissClicked(targetId: String) {
        viewModelScope.launch {
            smartspaceRepository.notifyDismissEvent(targetId)
        }
    }

    override fun onAboutClicked(aboutIntent: Intent, errorCallback: () -> Unit) {
        viewModelScope.launch {
            navigation.navigate(aboutIntent, errorCallback)
        }
    }

    override fun onFeedbackClicked(feedbackIntent: Intent, errorCallback: () -> Unit) {
        viewModelScope.launch {
            navigation.navigate(feedbackIntent, errorCallback)
        }
    }

    override fun onSettingsClicked(context: Context) {
        viewModelScope.launch {
            navigation.navigate(Intent(context, MainActivity::class.java))
        }
    }

    override fun onConfigureClicked(context: Context, appWidgetId: Int, owner: String) {
        viewModelScope.launch {
            val intent = ConfigurationActivity.createIntent(
                context, NavGraphMapping.WIDGET_SMARTSPACER
            ).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(SmartspacerWidgetConfigurationFragment.EXTRA_CALLING_PACKAGE, owner)
            }
            navigation.navigate(intent)
        }
    }

}