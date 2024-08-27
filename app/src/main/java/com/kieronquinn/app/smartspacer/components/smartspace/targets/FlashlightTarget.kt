package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.receivers.FlashlightReceiver
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.FlashlightRepository
import com.kieronquinn.app.smartspacer.repositories.FlashlightRepository.TargetState
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.sdk.annotations.LimitedNativeSupport
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.FlashlightToggleActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.Companion.createIntent
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import com.kieronquinn.app.smartspacer.utils.extensions.PendingIntent_MUTABLE_FLAGS
import com.kieronquinn.app.smartspacer.utils.extensions.hasFlashlight
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class FlashlightTarget: SmartspacerTargetProvider() {

    private val flashlightRepository by inject<FlashlightRepository>()
    private val dataRepository by inject<DataRepository>()
    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val data = dataRepository.getTargetData(smartspacerId, TargetData::class.java)
            ?: TargetData()
        return listOfNotNull(flashlightRepository.targetState.value.toTarget(data.recommend))
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            resources.getString(R.string.target_flashlight_title),
            resources.getString(R.string.target_flashlight_description),
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_flashlight),
            configActivity = createIntent(provideContext(), NavGraphMapping.TARGET_FLASHLIGHT),
            compatibilityState = getCompatibilityState(),
            allowAddingMoreThanOnce = true
        )
    }

    override fun createBackup(smartspacerId: String): Backup {
        val settings = dataRepository.getTargetData(smartspacerId, TargetData::class.java)
            ?: return Backup()
        val gson = get<Gson>()
        return Backup(gson.toJson(settings))
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val gson = get<Gson>()
        val settings = try {
            gson.fromJson(backup.data ?: return false, TargetData::class.java)
        }catch (e: Exception){
            return false
        }
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.FLASHLIGHT,
            ::restoreNotifyChange
        ){
            TargetData(settings.recommend)
        }
        return true
    }

    private fun restoreNotifyChange(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    @OptIn(LimitedNativeSupport::class)
    private fun TargetState.toTarget(recommend: Boolean): SmartspaceTarget? {
        return when {
            this == TargetState.ON -> TargetTemplate.Basic(
                "flashlight_at_${System.currentTimeMillis()}",
                ComponentName(provideContext(), this::class.java),
                SmartspaceTarget.FEATURE_FLASHLIGHT,
                Text(resources.getString(R.string.target_flashlight_title_on)),
                Text(resources.getString(R.string.target_flashlight_subtitle_on)),
                Icon(AndroidIcon.createWithResource(
                    provideContext(), R.drawable.ic_target_flashlight_off
                )),
                onClick = getTapAction()
            ).create().apply {
                hideSubtitleOnAod = true
                canBeDismissed = false
            }
            this == TargetState.OFF && recommend -> TargetTemplate.Basic(
                "flashlight_at_${System.currentTimeMillis()}",
                ComponentName(provideContext(), this::class.java),
                SmartspaceTarget.FEATURE_FLASHLIGHT,
                Text(resources.getString(R.string.target_flashlight_title_off)),
                Text(resources.getString(R.string.target_flashlight_subtitle_off)),
                Icon(AndroidIcon.createWithResource(
                    provideContext(), R.drawable.ic_target_flashlight_on
                )),
                onClick = getTapAction()
            ).create().apply {
                hideSubtitleOnAod = true
                canBeDismissed = false
            }
            else -> null
        }
    }

    private fun getTapAction(): TapAction {
        val pendingIntent = if(shizukuServiceRepository.isReady.value) {
            PendingIntent.getBroadcast(
                provideContext(),
                NotificationId.FLASHLIGHT.ordinal,
                Intent(provideContext(), FlashlightReceiver::class.java),
                PendingIntent_MUTABLE_FLAGS
            )
        }else{
            PendingIntent.getActivity(
                provideContext(),
                NotificationId.FLASHLIGHT.ordinal,
                Intent(provideContext(), FlashlightToggleActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                },
                PendingIntent_MUTABLE_FLAGS
            )
        }
        return TapAction(pendingIntent = pendingIntent, shouldShowOnLockScreen = true)
    }

    private fun getCompatibilityState(): CompatibilityState {
        return if(!provideContext().hasFlashlight()) {
            CompatibilityState.Incompatible(
                resources.getString(R.string.target_flashlight_incompatible)
            )
        }else CompatibilityState.Compatible
    }

    data class TargetData(
        @SerializedName("recommend")
        val recommend: Boolean = false
    )

}