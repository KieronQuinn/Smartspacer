package com.kieronquinn.app.smartspacer.components.smartspace.complications

import android.graphics.drawable.Icon
import android.os.Bundle
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultSmartspaceComponent
import org.koin.android.ext.android.inject

class DefaultComplication: SmartspacerComplicationProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.complication.default"
    }

    private val smartspaceRepository by inject<SmartspaceRepository>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()
    private val notificationRepository by inject<NotificationRepository>()
    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    private val smartspaceComponent by lazy {
        provideContext().getDefaultSmartspaceComponent()
    }

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        if(showShizukuNotificationIfNeeded()) return emptyList()
        val home = smartspaceRepository.getDefaultHomeActions().value.applyChanges().map {
            it.copy(limitToSurfaces = setOf(UiSurface.HOMESCREEN))
        }
        val lock = smartspaceRepository.getDefaultLockActions().value.applyChanges().map {
            it.copy(limitToSurfaces = setOf(UiSurface.LOCKSCREEN))
        }
        return home + lock
    }

    private fun List<SmartspaceAction>.applyChanges() = onEach {
        it.extras = Bundle().apply {
            putAll(it.extras)
            ComplicationTemplate.setSubcardTypeToWeather(this)
        }
    }

    private fun showShizukuNotificationIfNeeded(): Boolean {
        if(!shizukuServiceRepository.isReady.value && settingsRepository.enhancedMode.getSync()){
            notificationRepository.showShizukuNotification(
                R.string.notification_shizuku_content_at_a_glance_complication
            )
            return true
        }else{
            notificationRepository.cancelNotification(NotificationId.SHIZUKU)
        }
        return false
    }

    override fun getConfig(smartspacerId: String?): Config {
        val description = if(smartspacerId == null){
            R.string.complication_default_description_recommended
        }else{
            R.string.complication_default_description
        }
        return Config(
            label = resources.getString(R.string.complication_default_label),
            description = resources.getText(description),
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_target_default),
            compatibilityState = getCompatibilityState(),
            configActivity = TrampolineActivity.createAsiTrampolineIntent(provideContext()),
            allowAddingMoreThanOnce = true
        )
    }

    private fun getCompatibilityState(): CompatibilityState {
        return when {
            smartspaceComponent == null -> {
                CompatibilityState.Incompatible(
                    provideContext().getString(R.string.complication_default_description_unsupported)
                )
            }
            !settingsRepository.enhancedMode.getSync() -> {
                CompatibilityState.Incompatible(
                    provideContext().getString(R.string.complication_default_description_enhanced)
                )
            }
            else -> CompatibilityState.Compatible
        }
    }

}