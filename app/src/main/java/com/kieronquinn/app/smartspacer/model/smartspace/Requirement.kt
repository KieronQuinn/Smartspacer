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
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement.RequirementBackup.RequirementType
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.utils.extensions.callSafely
import com.kieronquinn.app.smartspacer.utils.extensions.invertIf
import com.kieronquinn.app.smartspacer.utils.extensions.observerAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Closeable

/**
 *  Represents a requirement coming from a given [authority] from a given [sourcePackage]
 */
class Requirement(
    val context: Context,
    val authority: String,
    val id: String?,
    val invert: Boolean,
    val sourcePackage: String = BuildConfig.APPLICATION_ID,
): KoinComponent, Closeable, Flow<Boolean> {

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
    private val contentResolver = context.contentResolver
    private val scope = MainScope()
    
    private val appChange = packageRepository.onPackageChanged(scope, sourcePackage)

    private val authorityBasedRemoteChange = contentResolver.observerAsFlow(authorityBasedUri)
    private val idBasedRemoteChange = contentResolver.observerAsFlow(idBasedUri)

    private val change = combine(appChange, authorityBasedRemoteChange, idBasedRemoteChange) { _, _, _ ->
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val remoteIsMet = change.mapLatest {
        getIsRequirementMet()
    }.invertIf(invert)

    private val remoteConfig = change.mapLatest {
        getRemoteConfig()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun getPluginConfig() = remoteConfig

    override suspend fun collect(collector: FlowCollector<Boolean>) {
        remoteIsMet.collect(collector)
    }

    private val defaultConfig = SmartspacerRequirementProvider.Config(
        context.resources.getString(R.string.plugin_requirement_default_label),
        context.resources.getString(R.string.requirement_not_available_alt),
        Icon.createWithResource(context, R.drawable.ic_target_action_default),
        compatibilityState = com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState.Incompatible(
            context.resources.getString(R.string.requirement_not_available)
        )
    )

    private suspend fun getIsRequirementMet(): Boolean {
        val requirement = callRemote(
            SmartspacerRequirementProvider.METHOD_GET,
            bundleOf(SmartspacerRequirementProvider.EXTRA_SMARTSPACER_ID to id)
        ) ?: return false
        if(requirement.isEmpty) return false
        return requirement.getBoolean(SmartspacerRequirementProvider.RESULT_KEY_REQUIREMENT_MET)
    }

    private suspend fun getRemoteConfig() = withContext(Dispatchers.IO) {
        val config = callRemote(
            SmartspacerRequirementProvider.METHOD_GET_CONFIG,
            bundleOf(SmartspacerRequirementProvider.EXTRA_SMARTSPACER_ID to id)
        ) ?: return@withContext defaultConfig
        if(config.isEmpty) return@withContext defaultConfig
        SmartspacerRequirementProvider.Config(config)
    }

    suspend fun onDeleted() {
        callRemote(
            SmartspacerRequirementProvider.METHOD_ON_REMOVED,
            bundleOf(
                SmartspacerRequirementProvider.EXTRA_SMARTSPACER_ID to id
            )
        )
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

    override fun close() {
        scope.cancel()
    }

    suspend fun createBackup(requirementType: RequirementType, requirementFor: String): RequirementBackup? {
        val id = id ?: return null
        val extras = bundleOf(SmartspacerRequirementProvider.EXTRA_SMARTSPACER_ID to id)
        return callRemote(SmartspacerRequirementProvider.METHOD_BACKUP, extras)?.let {
            Backup(it.getBundle(SmartspacerTargetProvider.EXTRA_BACKUP) ?: return null)
        }?.let {
            RequirementBackup(id, requirementFor, authority, it, requirementType, invert)
        }
    }

    suspend fun restoreBackup(backup: Backup): Boolean {
        val id = id ?: return false
        val extras = bundleOf(
            SmartspacerRequirementProvider.EXTRA_SMARTSPACER_ID to id,
            SmartspacerRequirementProvider.EXTRA_BACKUP to backup.toBundle()
        )
        return callRemote(SmartspacerRequirementProvider.METHOD_RESTORE, extras)
            ?.getBoolean(SmartspacerRequirementProvider.EXTRA_SUCCESS) ?: false
    }

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + authority.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + sourcePackage.hashCode()
        return result
    }

    @Parcelize
    data class RequirementBackup(
        @SerializedName("id")
        val id: String,
        @SerializedName("requirement_for")
        val requirementFor: String,
        @SerializedName("authority")
        val authority: String,
        @SerializedName("backup")
        val backup: Backup,
        @SerializedName("type")
        val requirementType: RequirementType,
        @SerializedName("invert")
        val invert: Boolean = false
    ): Parcelable {

        enum class RequirementType {
            ANY, ALL
        }

    }

}