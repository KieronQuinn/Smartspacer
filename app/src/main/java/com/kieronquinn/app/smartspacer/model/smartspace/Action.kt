package com.kieronquinn.app.smartspacer.model.smartspace

import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.utils.extensions.callSafely
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableArrayListCompat
import com.kieronquinn.app.smartspacer.utils.extensions.observerAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 *  Represents an action coming from a given [authority] from a given [sourcePackage] with a
 *  [config]
 */
class Action(
    val context: Context,
    val authority: String,
    val id: String?,
    val sourcePackage: String = BuildConfig.APPLICATION_ID,
    val config: Config = Config()
): KoinComponent, Flow<ActionHolder> {

    companion object {
        /**
         *  Debounce time for requirements to prevent glitching from too many updates
         */
        private const val REQUIREMENTS_DEBOUNCE = 1000L
    }

    private val idBasedUri = Uri.Builder()
        .scheme("content")
        .authority(authority)
        .appendPath(id)
        .build()

    private val authorityBasedUri = Uri.Builder()
        .scheme("content")
        .authority(authority)
        .build()

    private val scope = MainScope()
    private val packageRepository by inject<PackageRepository>()
    private val requirementsRepository by inject<RequirementsRepository>()
    private val contentResolver = context.contentResolver

    private val anyRawRequirements = requirementsRepository.getAnyRequirementsForComplication(id)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val anyRequirements = requirementsRepository.any(anyRawRequirements)
        .debounce(REQUIREMENTS_DEBOUNCE)
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val allRawRequirements = requirementsRepository.getAllRequirementsForComplication(id)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val allRequirements = requirementsRepository.all(allRawRequirements)
        .debounce(REQUIREMENTS_DEBOUNCE)
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun collect(collector: FlowCollector<ActionHolder>) {
        remoteActions.map {
            ActionHolder(this, it)
        }.collect(collector)
    }

    private val appChange = packageRepository.onPackageChanged(scope, sourcePackage)

    private val defaultConfig = SmartspacerComplicationProvider.Config(
        context.resources.getString(R.string.plugin_action_default_label),
        context.resources.getString(R.string.complication_not_available_alt),
        Icon.createWithResource(context, R.drawable.ic_target_action_default),
        compatibilityState = CompatibilityState.Incompatible(
            context.resources.getString(R.string.complication_not_available_alt)
        )
    )

    private val authorityBasedRemoteChange = contentResolver.observerAsFlow(authorityBasedUri)
    private val idBasedRemoteChange = contentResolver.observerAsFlow(idBasedUri)

    private val change = combine(appChange, authorityBasedRemoteChange, idBasedRemoteChange) { _, _, _ ->
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val remoteActions = combine(change, anyRequirements, allRequirements){ _, any, all ->
        if(any && all){
            getRemoteActions()
        }else{
            emptyList()
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val remoteConfig = change.map {
        getRemoteConfig()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun getPluginConfig() = remoteConfig

    suspend fun onDeleted() {
        callRemote(
            SmartspacerComplicationProvider.METHOD_ON_REMOVED,
            bundleOf(
                SmartspacerComplicationProvider.EXTRA_SMARTSPACER_ID to id
            )
        )
    }

    private suspend fun getRemoteActions(): List<SmartspaceAction> {
        val actions = callRemote(
            SmartspacerComplicationProvider.METHOD_GET,
            bundleOf(SmartspacerComplicationProvider.EXTRA_SMARTSPACER_ID to id)
        ) ?: return emptyList()
        if(actions.isEmpty) return emptyList()
        return actions.getParcelableArrayListCompat(
            SmartspacerComplicationProvider.RESULT_KEY_SMARTSPACE_ACTIONS, Bundle::class.java
        )?.map { SmartspaceAction(it) } ?: emptyList()
    }

    private suspend fun getRemoteConfig() = withContext(Dispatchers.IO) {
        val config = callRemote(
            SmartspacerComplicationProvider.METHOD_GET_CONFIG,
            bundleOf(SmartspacerComplicationProvider.EXTRA_SMARTSPACER_ID to id)
        ) ?: return@withContext defaultConfig
        if(config.isEmpty) return@withContext defaultConfig
        SmartspacerComplicationProvider.Config(config)
    }

    private suspend fun callRemote(
        method: String, extras: Bundle? = null
    ): Bundle? = withContext(Dispatchers.IO) {
        try {
            contentResolver?.callSafely(authority, method, null, extras)
        }catch (e: Throwable){
            //Provider has gone
            null
        }
    }

    fun close() {
        allRawRequirements.value.forEach {
            it.close()
        }
        anyRawRequirements.value.forEach {
            it.close()
        }
        scope.cancel()
    }

    suspend fun createBackup(): ComplicationBackup? {
        val id = id ?: return null
        val extras = bundleOf(SmartspacerComplicationProvider.EXTRA_SMARTSPACER_ID to id)
        return callRemote(SmartspacerComplicationProvider.METHOD_BACKUP, extras)?.let {
            Backup(it.getBundle(SmartspacerComplicationProvider.EXTRA_BACKUP) ?: return null)
        }?.let {
            ComplicationBackup(id, authority, it, config)
        }
    }

    suspend fun restoreBackup(backup: Backup): Boolean {
        val id = id ?: return false
        val extras = bundleOf(
            SmartspacerComplicationProvider.EXTRA_SMARTSPACER_ID to id,
            SmartspacerComplicationProvider.EXTRA_BACKUP to backup.toBundle()
        )
        return callRemote(SmartspacerComplicationProvider.METHOD_RESTORE, extras)
            ?.getBoolean(SmartspacerComplicationProvider.EXTRA_SUCCESS) ?: false
    }

    override fun equals(other: Any?): Boolean {
        return false
    }

    private fun setupRequirementsLifecycle() {
        scope.launch {
            var allRequirementsCache = allRawRequirements.value
            allRawRequirements.collect {
                allRequirementsCache.forEach { target -> target.close() }
                allRequirementsCache = it
            }
        }
        scope.launch {
            var anyRawRequirementsCache = anyRawRequirements.value
            anyRawRequirements.collect {
                anyRawRequirementsCache.forEach { target -> target.close() }
                anyRawRequirementsCache = it
            }
        }
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + authority.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + sourcePackage.hashCode()
        result = 31 * result + config.hashCode()
        return result
    }

    init {
        setupRequirementsLifecycle()
    }

    @Parcelize
    data class Config(
        @SerializedName("show_on_home")
        val showOnHomeScreen: Boolean = true,
        @SerializedName("show_on_lock")
        val showOnLockScreen: Boolean = true,
        @SerializedName("show_on_expanded")
        val showOnExpanded: Boolean = true,
        @SerializedName("show_over_music")
        val showOverMusic: Boolean = false,
        @SerializedName("expanded_show_when_locked")
        val expandedShowWhenLocked: Boolean = true
    ): Parcelable

    @Parcelize
    data class ComplicationBackup(
        @SerializedName("id")
        val id: String,
        @SerializedName("authority")
        val authority: String,
        @SerializedName("backup")
        val backup: Backup,
        @SerializedName("config")
        val config: Config
    ): Parcelable

}

data class ActionHolder(val parent: Action, val actions: List<SmartspaceAction>?) {
    override fun equals(other: Any?): Boolean {
        return false
    }
}