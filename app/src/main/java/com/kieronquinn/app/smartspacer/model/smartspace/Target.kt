package com.kieronquinn.app.smartspacer.model.smartspace

import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.Companion.EXTRA_DID_DISMISS
import com.kieronquinn.app.smartspacer.utils.extensions.callSafely
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableArrayListCompat
import com.kieronquinn.app.smartspacer.utils.extensions.observerAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Closeable
import com.kieronquinn.app.smartspacer.sdk.client.R as ClientR

/**
 *  Represents a target coming from a given [authority] from a given [sourcePackage] with a
 *  [config]
 */
class Target(
    val context: Context,
    val authority: String,
    val id: String?,
    val sourcePackage: String = BuildConfig.APPLICATION_ID,
    val config: Config = Config()
): KoinComponent, Closeable, Flow<TargetHolder> {

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

    private val packageRepository by inject<PackageRepository>()
    private val requirementsRepository by inject<RequirementsRepository>()
    private val contentResolver = context.contentResolver
    private val scope = MainScope()

    private val anyRawRequirements = requirementsRepository.getAnyRequirementsForTarget(id)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val anyRequirements = requirementsRepository.any(anyRawRequirements)
        .debounce(REQUIREMENTS_DEBOUNCE)
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val allRawRequirements = requirementsRepository.getAllRequirementsForTarget(id)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val allRequirements = requirementsRepository.all(allRawRequirements)
        .debounce(REQUIREMENTS_DEBOUNCE)
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val appChange = packageRepository.onPackageChanged(scope, sourcePackage)

    override suspend fun collect(collector: FlowCollector<TargetHolder>) {
        remoteTargets.map {
            TargetHolder(this, it)
        }.collect(collector)
    }

    private val defaultConfig = SmartspacerTargetProvider.Config(
        context.resources.getString(R.string.plugin_target_default_label),
        context.resources.getString(R.string.target_not_available_alt),
        Icon.createWithResource(context, R.drawable.ic_target_action_default),
        compatibilityState = CompatibilityState.Incompatible(
            context.resources.getString(R.string.target_not_available_alt)
        )
    )

    private val authorityBasedRemoteChange = contentResolver.observerAsFlow(authorityBasedUri)
    private val idBasedRemoteChange = contentResolver.observerAsFlow(idBasedUri)

    private val change = combine(appChange, authorityBasedRemoteChange, idBasedRemoteChange) { _, _, _ ->
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val remoteTargets = combine(change, anyRequirements, allRequirements){ _, any, all ->
        if(any && all){
            getRemoteTargets()
        }else{
            emptyList()
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val remoteConfig = change.map {
        getRemoteConfig()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun getPluginConfig() = remoteConfig

    suspend fun onDismiss(targetId: String){
        val didDismiss = callRemote(
            SmartspacerTargetProvider.METHOD_DISMISS,
            bundleOf(
                SmartspacerTargetProvider.EXTRA_TARGET_ID to targetId,
                SmartspacerTargetProvider.EXTRA_SMARTSPACER_ID to id
            )
        )?.getBoolean(EXTRA_DID_DISMISS) ?: true
        if(!didDismiss){
            Toast.makeText(
                context, ClientR.string.smartspace_toast_could_not_dismiss, Toast.LENGTH_LONG
            ).show()
        }
    }

    suspend fun onDeleted() {
        callRemote(
            SmartspacerTargetProvider.METHOD_ON_REMOVED,
            bundleOf(
                SmartspacerTargetProvider.EXTRA_SMARTSPACER_ID to id
            )
        )
    }

    private suspend fun getRemoteTargets() = withContext(Dispatchers.IO) {
        val targets = callRemote(
            SmartspacerTargetProvider.METHOD_GET,
            bundleOf(SmartspacerTargetProvider.EXTRA_SMARTSPACER_ID to id)
        ) ?: return@withContext emptyList()
        if(targets.isEmpty) return@withContext emptyList()
        targets.getParcelableArrayListCompat(
            SmartspacerTargetProvider.RESULT_KEY_SMARTSPACE_TARGETS, Bundle::class.java
        )?.map { SmartspaceTarget(it) } ?: emptyList()
    }

    private suspend fun getRemoteConfig() = withContext(Dispatchers.IO) {
        val config = callRemote(
            SmartspacerTargetProvider.METHOD_GET_CONFIG,
            bundleOf(SmartspacerTargetProvider.EXTRA_SMARTSPACER_ID to id)
        ) ?: return@withContext defaultConfig
        if(config.isEmpty) return@withContext defaultConfig
        SmartspacerTargetProvider.Config(config)
    }

    private suspend fun callRemote(
        method: String, extras: Bundle? = null
    ): Bundle? = withContext(Dispatchers.IO) {
        try {
            contentResolver?.callSafely(authority, method, null, extras)
        }catch (e: Throwable){
            //Provider has gone
            Log.w("Target", Log.getStackTraceString(e))
            null
        }
    }

    override fun close() {
        allRawRequirements.value.forEach {
            it.close()
        }
        anyRawRequirements.value.forEach {
            it.close()
        }
        scope.cancel()
    }

    suspend fun createBackup(): TargetBackup? {
        val id = id ?: return null
        val extras = bundleOf(SmartspacerTargetProvider.EXTRA_SMARTSPACER_ID to id)
        return callRemote(SmartspacerTargetProvider.METHOD_BACKUP, extras)?.let {
            Backup(it.getBundle(SmartspacerTargetProvider.EXTRA_BACKUP) ?: return null)
        }?.let {
            TargetBackup(id, authority, it, config)
        }
    }

    suspend fun restoreBackup(backup: Backup): Boolean {
        val id = id ?: return false
        val extras = bundleOf(
            SmartspacerTargetProvider.EXTRA_SMARTSPACER_ID to id,
            SmartspacerTargetProvider.EXTRA_BACKUP to backup.toBundle()
        )
        return callRemote(SmartspacerTargetProvider.METHOD_RESTORE, extras)
            ?.getBoolean(SmartspacerTargetProvider.EXTRA_SUCCESS) ?: false
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
        @SerializedName("show_remote_views")
        val showRemoteViews: Boolean = true,
        @SerializedName("show_widget")
        val showWidget: Boolean = true,
        @SerializedName("show_shortcuts")
        val showShortcuts: Boolean = true,
        @SerializedName("show_app_shortcuts")
        val showAppShortcuts: Boolean = true,
        @SerializedName("expanded_show_when_locked")
        val expandedShowWhenLocked: Boolean = true,
        @SerializedName("disable_sub_complications")
        val disableSubComplications: Boolean = false
    ): Parcelable

    @Parcelize
    data class TargetBackup(
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

data class TargetHolder(val parent: Target, val targets: List<SmartspaceTarget>?) {
    override fun equals(other: Any?): Boolean {
        return false
    }
}