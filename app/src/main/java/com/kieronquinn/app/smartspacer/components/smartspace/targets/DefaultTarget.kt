package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.Context
import android.graphics.drawable.Icon
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.*
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping.TARGET_DEFAULT
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultSmartspaceComponent
import org.koin.android.ext.android.inject

/**
 *  Displays targets from the default Smartspace provider
 */
class DefaultTarget: SmartspacerTargetProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.target.default"
    }

    private val smartspaceRepository by inject<SmartspaceRepository>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()
    private val notificationRepository by inject<NotificationRepository>()
    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()
    private val dataRepository by inject<DataRepository>()
    private val gson by inject<Gson>()

    private val smartspaceComponent by lazy {
        provideContext().getDefaultSmartspaceComponent()
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        if(showShizukuNotificationIfNeeded()) return emptyList()
        val settings = getSettings(smartspacerId) ?: TargetData()
        val home = smartspaceRepository.getDefaultHomeTargets().value.filter(settings).map {
            it.copy(limitToSurfaces = setOf(UiSurface.HOMESCREEN))
        }
        val lock = smartspaceRepository.getDefaultLockTargets().value.filter(settings).map {
            it.copy(limitToSurfaces = setOf(UiSurface.LOCKSCREEN))
        }
        return home + lock
    }

    private fun List<SmartspaceTarget>.filter(settings: TargetData) = filterNot {
        settings.hiddenTargetTypes.contains(it.getTargetType())
    }

    private fun showShizukuNotificationIfNeeded(): Boolean {
        if(!shizukuServiceRepository.isReady.value && settingsRepository.enhancedMode.getSync()){
            notificationRepository.showShizukuNotification(
                R.string.notification_shizuku_content_at_a_glance_target
            )
            return true
        }else{
            notificationRepository.cancelNotification(NotificationId.SHIZUKU)
        }
        return false
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        smartspaceRepository.dismissDefaultTarget(targetId)
        return true
    }

    override fun createBackup(smartspacerId: String): Backup {
        val settings = getSettings(smartspacerId) ?: return Backup()
        return Backup(gson.toJson(settings), null)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val settings = gson.fromJson(backup.data ?: return false, TargetData::class.java)
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.DEFAULT,
            ::onBackupRestored
        ) {
            val data = it ?: TargetData()
            data.copy(hiddenTargetTypes = settings.hiddenTargetTypes)
        }
        return true
    }

    private fun onBackupRestored(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    override fun getConfig(smartspacerId: String?): Config {
        val description = if(smartspacerId == null){
            R.string.target_default_description_recommended
        }else{
            R.string.target_default_description
        }
        return Config(
            label = resources.getString(R.string.target_default_label),
            description = resources.getText(description),
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_target_default),
            compatibilityState = getCompatibilityState(),
            configActivity = TrampolineActivity.createAsiTrampolineIntent(provideContext())?.let {
                ConfigurationActivity.createIntent(provideContext(), TARGET_DEFAULT)
            }
        )
    }

    private fun getCompatibilityState(): CompatibilityState {
        return when {
            smartspaceComponent == null -> {
                CompatibilityState.Incompatible(
                    provideContext().getString(R.string.target_default_description_unsupported)
                )
            }
            !settingsRepository.enhancedMode.getSync() -> {
                CompatibilityState.Incompatible(
                    provideContext().getString(R.string.target_default_description_enhanced)
                )
            }
            else -> CompatibilityState.Compatible
        }
    }

    private fun SmartspaceTarget.getTargetType(): String {
        val id = templateData?.primaryItem?.tapAction?.id?.takeIf { it.isNotBlank() }
            ?: headerAction?.id?.takeIf { it.isNotBlank() } ?: smartspaceTargetId
        return if(id.contains("-")) {
            id.split("-")[0]
        }else id.toString()
    }

    private fun getSettings(smartspacerId: String): TargetData? {
        return dataRepository.getTargetData(smartspacerId, TargetData::class.java)
    }

    data class TargetData(
        @SerializedName("hidden_target_types")
        val hiddenTargetTypes: Set<String> = emptySet()
    )

}