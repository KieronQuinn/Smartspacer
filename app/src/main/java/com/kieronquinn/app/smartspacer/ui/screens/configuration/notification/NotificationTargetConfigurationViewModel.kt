package com.kieronquinn.app.smartspacer.ui.screens.configuration.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.notifications.NotificationTargetNotification
import com.kieronquinn.app.smartspacer.components.smartspace.targets.NotificationTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.NotificationTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
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
    abstract fun onTrimNewLinesChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data object GrantNotificationAccess: State()
        data object GrantAssociation: State()
        data class Settings(
            val selectedAppLabel: CharSequence?,
            val availableChannels: Map<NotificationChannelGroup?, List<NotificationChannel>>,
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

    @SuppressLint("NewApi")
    private val channels = settings.mapLatest {
        val packageName = it?.packageName ?: return@mapLatest emptyList()
        notificationRepository.getNotificationChannelsForPackage(packageName)
    }

    private val groups = settings.mapLatest {
        val packageName = it?.packageName ?: return@mapLatest emptyList()
        notificationRepository.getNotificationChannelGroupsForPackage(packageName)
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
        channels,
        groups
    ) { listenerEnabled, channelsAvailable, settings, notificationChannels, notificationGroups ->
        val channels = notificationChannels.groupBy { notification ->
            notificationGroups.firstOrNull { it.id == notification.group }
        }
        when {
            !listenerEnabled -> State.GrantNotificationAccess
            !channelsAvailable -> State.GrantAssociation
            else -> {
                val appName = settings.packageName?.let {
                    packageManager.getPackageLabel(it)
                }
                State.Settings(appName, channels, settings)
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
            val data = it ?: TargetData()
            data.copy(
                hasChannels = true,
                channels = newChannels
            )
        }
    }

    override fun onTrimNewLinesChanged(enabled: Boolean) {
        val id = smartspacerId.value ?: return
        dataRepository.updateTargetData(
            id,
            TargetData::class.java,
            TargetDataType.NOTIFICATION,
            ::onSettingsUpdated
        ) {
            val data = it ?: TargetData()
            data.copy(
                _trimNewLines = enabled
            )
        }
    }

    private fun onSettingsUpdated(context: Context, smartspacerId: String) {
        SmartspacerNotificationProvider.notifyChange(
            context, NotificationTargetNotification::class.java, smartspacerId
        )
        SmartspacerTargetProvider.notifyChange(
            context, NotificationTarget::class.java, smartspacerId
        )
    }

}