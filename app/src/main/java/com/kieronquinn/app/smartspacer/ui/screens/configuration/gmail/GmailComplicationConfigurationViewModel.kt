package com.kieronquinn.app.smartspacer.ui.screens.configuration.gmail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.complications.GmailComplication
import com.kieronquinn.app.smartspacer.components.smartspace.complications.GmailComplication.ActionData
import com.kieronquinn.app.smartspacer.model.database.ActionDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.GmailRepository
import com.kieronquinn.app.smartspacer.repositories.GmailRepository.Label
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class GmailComplicationConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setupWithId(smartspacerId: String)
    abstract fun setHasGrantedPermission(granted: Boolean)
    abstract fun onAccountSelected(account: String)
    abstract fun onLabelChanged(label: Label, enabled: Boolean)
    abstract fun launchPermissionSettings(context: Context)

    sealed class State {
        object Loading: State()
        data class Loaded(val settings: ActionData, val labels: List<Label>): State()
    }

}

class GmailComplicationConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    private val gmailRepository: GmailRepository,
    scope: CoroutineScope? = null
): GmailComplicationConfigurationViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)
    private val hasGrantedPermission = MutableStateFlow<Boolean?>(null)

    private val data = id.filterNotNull().flatMapLatest {
        dataRepository.getActionDataFlow(it, ActionData::class.java).map { data ->
            data ?: ActionData(it)
        }
    }.flowOn(Dispatchers.IO)

    private val account = data.mapLatest {
        it.accountName
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    private val labels = combine(
        account,
        hasGrantedPermission.filterNotNull()
    ) { account, hasGranted ->
        if(!hasGranted) return@combine emptyList()
        gmailRepository.getAllLabels(account ?: return@combine emptyList())
    }.flowOn(Dispatchers.IO)

    override val state = combine(
        data,
        labels
    ) { settings, allLabels ->
        State.Loaded(settings, allLabels)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            id.emit(smartspacerId)
        }
    }

    override fun setHasGrantedPermission(granted: Boolean) {
        vmScope.launch {
            hasGrantedPermission.emit(granted)
            gmailRepository.reload()
        }
    }

    override fun onAccountSelected(account: String) {
        val current = this.account.value
        if(current == account) return //Prevent updating if it's identical so we don't reset
        updateActionData {
            it.copy(accountName = account, enabledLabels = emptySet())
        }
    }

    override fun onLabelChanged(label: Label, enabled: Boolean) {
        updateActionData {
            val current = it.enabledLabels
            if(enabled){
                it.copy(enabledLabels = current.plus(label.canonicalName))
            }else{
                it.copy(enabledLabels = current.minus(label.canonicalName))
            }
        }
    }

    override fun launchPermissionSettings(context: Context) {
        vmScope.launch {
            Toast.makeText(context, R.string.complication_gmail_settings_toast, Toast.LENGTH_LONG)
                .show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            }
            navigation.navigate(intent)
            delay(500L)
            navigation.finish()
        }
    }

    private fun updateActionData(block: (ActionData) -> ActionData) {
        val id = id.value ?: return
        dataRepository.updateActionData(
            id,
            ActionData::class.java,
            ActionDataType.GMAIL,
            ::onActionUpdated
        ) {
            val data = it ?: ActionData(id)
            block(data)
        }
    }

    private fun onActionUpdated(context: Context, smartspacerId: String) {
        SmartspacerComplicationProvider.notifyChange(
            context, GmailComplication::class.java, smartspacerId
        )
    }

}