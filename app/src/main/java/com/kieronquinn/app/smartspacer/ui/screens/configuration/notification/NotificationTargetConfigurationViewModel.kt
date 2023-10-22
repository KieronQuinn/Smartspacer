package com.kieronquinn.app.smartspacer.ui.screens.configuration.notification

import android.app.NotificationChannel
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.notifications.NotificationTargetNotification
import com.kieronquinn.app.smartspacer.components.smartspace.targets.NotificationTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class NotificationTargetConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setupWithId(smartspacerId: String)
    abstract fun onResume()

    abstract fun onReadMoreClicked()
    abstract fun onAppClicked()
    abstract fun onChannelChanged(channelId: String, enabled: Boolean)

    sealed class State {
        object Loading: State()
        object GrantNotificationAccess: State()
        object GrantAssociation: State()
        data class Settings(
            val selectedAppLabel: CharSequence?,
            val availableChannels: List<NotificationChannel>,
            val options: TargetData
        ): State()
    }

}

class NotificationTargetConfigurationViewModelImpl(
    private val notificationRepository: NotificationRepository,
    private val navigation: ConfigurationNavigation,
    private val dataRepository: DataRepository,
    context: Context,
    scope: CoroutineScope? = null
): NotificationTargetConfigurationViewModel(scope) {

    private val packageManager = context.packageManager
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val smartspacerId = MutableStateFlow<String?>(null)

    private val settings = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getTargetDataFlow(it, TargetData::class.java).mapLatest { data ->
            data ?: TargetData()
        }
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    private val notificationListenerEnabled = resumeBus.mapLatest {
        notificationRepository.isNotificationListenerEnabled()
    }

    private val notificationChannelsAvailable = resumeBus.mapLatest {
        notificationRepository.getNotificationChannelsAvailable()
    }

    private val channels = settings.mapLatest {
        val packageName = it?.packageName ?: return@mapLatest emptyList()
        notificationRepository.getNotificationChannelsForPackage(packageName)
    }

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            this@NotificationTargetConfigurationViewModelImpl.smartspacerId.emit(smartspacerId)
        }
    }

    override fun onResume() {
        vmScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override val state = combine(
        notificationListenerEnabled,
        notificationChannelsAvailable,
        settings.filterNotNull(),
        channels
    ) { listenerEnabled, channelsAvailable, settings, notificationChannels ->
        when {
            !listenerEnabled -> State.GrantNotificationAccess
            !channelsAvailable -> State.GrantAssociation
            else -> {
                val appName = settings.packageName?.let {
                    packageManager.getPackageLabel(it)
                }
                State.Settings(appName, notificationChannels, settings)
            }
        }
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onReadMoreClicked() {
        vmScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://twitter.com/MishaalRahman/status/1573072946205462529")
            })
        }
    }

    override fun onAppClicked() {
        val id = smartspacerId.value ?: return
        vmScope.launch {
            navigation.navigate(
                NotificationTargetConfigurationFragmentDirections.actionNotificationTargetConfigurationFragmentToNotificationTargetConfigurationAppPickerFragment(id)
            )
        }
    }

    override fun onChannelChanged(channelId: String, enabled: Boolean) {
        val settings = settings.value ?: return
        val id = smartspacerId.value ?: return
        dataRepository.updateTargetData(
            id,
            TargetData::class.java,
            TargetDataType.NOTIFICATION,
            ::onSettingsUpdated
        ) {
            val newChannels = if(enabled){
                settings.channels + channelId
            }else{
                settings.channels - channelId
            }
            TargetData(settings.packageName, true, newChannels)
        }
    }

    private fun onSettingsUpdated(context: Context, smartspacerId: String) {
        SmartspacerNotificationProvider.notifyChange(
            context, NotificationTargetNotification::class.java, smartspacerId
        )
    }

}