package com.kieronquinn.app.smartspacer.ui.screens.permissions

import android.content.Context
import androidx.annotation.StringRes
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.AllowAskEveryTimeOptions
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.AllowDenyOptions
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.Card
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.Header
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.NotificationsPermission
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.SmartspacePermission
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.WidgetPermission
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class PermissionsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onWidgetPermissionSet(
        packageName: String,
        allowAskEveryTimeOptions: AllowAskEveryTimeOptions
    )

    abstract fun onNotificationsPermissionSet(
        packageName: String,
        allowAskEveryTimeOptions: AllowAskEveryTimeOptions
    )

    abstract fun onSmartspacePermissionSet(
        packageName: String,
        allowDenyOptions: AllowDenyOptions
    )

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<PermissionItem>): State()
    }

    sealed class PermissionItem {
        data class Header(@StringRes val title: Int): PermissionItem()
        data class Card(@StringRes val content: Int): PermissionItem()

        data class WidgetPermission(
            val packageName: String,
            val title: CharSequence
        ): PermissionItem()

        data class NotificationsPermission(
            val packageName: String,
            val title: CharSequence
        ): PermissionItem()

        data class SmartspacePermission(
            val packageName: String,
            val title: CharSequence
        ): PermissionItem()

        enum class AllowAskEveryTimeOptions(@StringRes val title: Int) {
            ALLOW(R.string.permissions_allow),
            ASK_EVERY_TIME(R.string.permissions_ask_every_time)
        }

        enum class AllowDenyOptions(@StringRes val title: Int) {
            ALLOW(R.string.permissions_allow),
            DENY(R.string.permissions_deny)
        }
    }

}

class PermissionsViewModelImpl(
    context: Context,
    private val grantRepository: GrantRepository,
    scope: CoroutineScope? = null
): PermissionsViewModel(scope) {

    private val packageManager = context.packageManager

    override val state = grantRepository.grants.mapLatest {
        val grants = it ?: emptyList()
        val items = ArrayList<PermissionItem>()
        val widgetGrants = grants.filter { grant -> grant.widget }.mapNotNull { grant ->
            val label = packageManager.getPackageLabel(grant.packageName) ?: return@mapNotNull null
            WidgetPermission(grant.packageName, label)
        }
        val notificationsGrants = grants.filter { grant -> grant.notifications }.mapNotNull { grant ->
            val label = packageManager.getPackageLabel(grant.packageName) ?: return@mapNotNull null
            NotificationsPermission(grant.packageName, label)
        }
        val smarspaceGrants = grants.filter { grant -> grant.smartspace }.mapNotNull { grant ->
            val label = packageManager.getPackageLabel(grant.packageName) ?: return@mapNotNull null
            SmartspacePermission(grant.packageName, label)
        }
        if(widgetGrants.isNotEmpty() || notificationsGrants.isNotEmpty()){
            items.add(Card(R.string.permissions_header))
        }
        if(widgetGrants.isNotEmpty()){
            items.add(Header(R.string.permissions_widgets))
            items.addAll(widgetGrants)
        }
        if(notificationsGrants.isNotEmpty()){
            items.add(Header(R.string.permissions_notifications))
            items.addAll(notificationsGrants)
        }
        if(smarspaceGrants.isNotEmpty()){
            items.add(Header(R.string.permissions_smartspace))
            items.addAll(smarspaceGrants)
        }
        State.Loaded(items)
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onWidgetPermissionSet(
        packageName: String,
        allowAskEveryTimeOptions: AllowAskEveryTimeOptions
    ) {
        //Only update if being revoked
        if(allowAskEveryTimeOptions != AllowAskEveryTimeOptions.ASK_EVERY_TIME) return
        vmScope.launch {
            val grant = grantRepository.getGrantForPackage(packageName) ?: Grant(packageName)
            grantRepository.addGrant(grant.copy(widget = false))
        }
    }

    override fun onNotificationsPermissionSet(
        packageName: String,
        allowAskEveryTimeOptions: AllowAskEveryTimeOptions
    ) {
        //Only update if being revoked
        if(allowAskEveryTimeOptions != AllowAskEveryTimeOptions.ASK_EVERY_TIME) return
        vmScope.launch {
            val grant = grantRepository.getGrantForPackage(packageName) ?: Grant(packageName)
            grantRepository.addGrant(grant.copy(notifications = false))
        }
    }

    override fun onSmartspacePermissionSet(
        packageName: String,
        allowDenyOptions: AllowDenyOptions
    ) {
        //Only update if being revoked
        if(allowDenyOptions != AllowDenyOptions.DENY) return
        vmScope.launch {
            val grant = grantRepository.getGrantForPackage(packageName) ?: Grant(packageName)
            grantRepository.addGrant(grant.copy(smartspace = false))
        }
    }

}