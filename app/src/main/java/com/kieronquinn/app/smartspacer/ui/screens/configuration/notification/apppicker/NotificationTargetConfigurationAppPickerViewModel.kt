package com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.apppicker

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.notifications.NotificationTargetNotification
import com.kieronquinn.app.smartspacer.components.smartspace.targets.NotificationTarget
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository.ListAppsApp
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class NotificationTargetConfigurationAppPickerViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val showSearchClear: StateFlow<Boolean>

    abstract fun setSearchTerm(term: String)
    abstract fun getSearchTerm(): String

    abstract fun onAppSelected(id: String, packageName: String)

    sealed class State {
        object Loading: State()
        data class Loaded(val apps: List<ListAppsApp>): State()
    }

}

class NotificationTargetConfigurationAppPickerViewModelImpl(
    private val navigation: ConfigurationNavigation,
    private val notificationRepository: NotificationRepository,
    private val dataRepository: DataRepository,
    packageRepository: PackageRepository,
    scope: CoroutineScope? = null
): NotificationTargetConfigurationAppPickerViewModel(scope) {

    private val allApps = flow {
        emit(packageRepository.getInstalledApps(true))
    }.flowOn(Dispatchers.IO)

    @VisibleForTesting
    val searchTerm = MutableStateFlow("")

    override val showSearchClear = searchTerm.map { it.isNotBlank() }
        .stateIn(vmScope, SharingStarted.Eagerly, false)

    override val state = combine(allApps, searchTerm) { all, term ->
        State.Loaded(all.filter {
            it.label.contains(term, true) || it.packageName.contains(term, true)
        })
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(term: String) {
        vmScope.launch {
            searchTerm.emit(term)
        }
    }

    override fun onAppSelected(id: String, packageName: String) {
        dataRepository.updateTargetData(
            id,
            NotificationTarget.TargetData::class.java,
            TargetDataType.NOTIFICATION,
            ::onDataSaved
        ) {
            //If the same app is selected, just return that (prevent overwriting channels)
            if(it?.packageName == packageName) {
                return@updateTargetData it
            }
            //Set the data with no channels enabled
            val channels = getAllChannels(packageName)
            NotificationTarget.TargetData(
                packageName = packageName,
                hasChannels = channels.isNotEmpty(),
                _trimNewLines = false, //Disabled by default for new selections
                channels = emptySet()
            )
        }
    }

    private fun getAllChannels(packageName: String): Set<String> {
        return notificationRepository.getNotificationChannelsForPackage(packageName).map {
            it.id
        }.toSet()
    }

    private fun onDataSaved(context: Context, smartspacerId: String) {
        SmartspacerNotificationProvider.notifyChange(
            context, NotificationTargetNotification::class.java, smartspacerId
        )
        vmScope.launch {
            navigation.navigateBack()
        }
    }

}