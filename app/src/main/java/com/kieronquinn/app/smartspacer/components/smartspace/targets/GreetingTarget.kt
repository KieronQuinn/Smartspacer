package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.StringRes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.annotations.LimitedNativeSupport
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(LimitedNativeSupport::class)
class GreetingTarget: SmartspacerTargetProvider() {

    companion object {
        fun getNextGreetingChangeTime(): Instant {
            return Greeting.getNextChange()
        }
    }

    private val dataRepository by inject<DataRepository>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val settings = dataRepository.getTargetData(smartspacerId, TargetData::class.java)
            ?: TargetData(name = settingsRepository.userName.getSync())
        return listOf(createTarget(settings, smartspacerId))
    }

    private fun createTarget(settings: TargetData, smartspacerId: String): SmartspaceTarget {
        val greeting = Greeting.getGreeting().let {
            if(settings.name.isNotBlank()){
                resources.getString(it.namedGreeting, settings.name)
            }else{
                resources.getString(it.greeting)
            }
        }
        val onClick = if(settings.openExpandedOnClick) {
            TapAction(intent = Intent("com.kieronquinn.app.smartspacer.SMARTSPACE").apply {
                component = ComponentName(
                    BuildConfig.APPLICATION_ID,
                    "com.kieronquinn.app.smartspacer.ui.activities.ExportedExpandedActivity"
                )
            }, shouldShowOnLockScreen = true)
        }else null
        return TargetTemplate.Basic(
            "greeting_$smartspacerId",
            ComponentName(provideContext(), GreetingTarget::class.java),
            title = Text(greeting),
            subtitle = null,
            icon = null,
            onClick = onClick
        ).create().apply {
            hideIfNoComplications = settings.hideIfNoComplications
            hideTitleOnAod = settings.hideTitleOnAod
            canTakeTwoComplications = true
            canBeDismissed = false
        }
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        //This target cannot be dismissed
        return false
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.target_greeting_label),
            description = resources.getString(R.string.target_greeting_description),
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_target_greeting),
            allowAddingMoreThanOnce = true,
            configActivity = ConfigurationActivity.createIntent(
                provideContext(), ConfigurationActivity.NavGraphMapping.TARGET_GREETING
            )
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
            TargetDataType.GREETING,
            ::restoreNotifyChange
        ){
            TargetData(settings.name, settings.hideIfNoComplications, settings.hideTitleOnAod)
        }
        return true
    }

    private fun restoreNotifyChange(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private enum class Greeting(
        @StringRes val greeting: Int,
        @StringRes val namedGreeting: Int,
        val startsAtHour: Int,
        val endsAtHour: Int
    ) {
        MORNING(
            R.string.target_greeting_morning,
            R.string.target_greeting_morning_name,
            3,
            12
        ),
        AFTERNOON(
            R.string.target_greeting_afternoon,
            R.string.target_greeting_afternoon_name,
            12,
            18
        ),
        EVENING(
            R.string.target_greeting_evening,
            R.string.target_greeting_evening_name,
            18,
            22
        ),
        NIGHT(
            R.string.target_greeting_night,
            R.string.target_greeting_night_name,
            22,
            3
        );

        companion object {
            fun getGreeting(): Greeting {
                val hour = LocalDateTime.now().hour
                return values().firstOrNull {
                    hour >= it.startsAtHour && hour < it.endsAtHour
                } ?: NIGHT
            }

            fun getNextChange(): Instant {
                val current = getGreeting()
                val next = values().getOrNull(current.ordinal + 1) ?: MORNING
                val hour = LocalTime.of(next.startsAtHour, 0)
                return if(hour.atDate(LocalDate.now()).isBefore(LocalDateTime.now())){
                    hour.atDate(LocalDate.now().plusDays(1))
                }else{
                    hour.atDate(LocalDate.now())
                }.atZone(ZoneId.systemDefault()).toInstant()
            }
        }
    }

    data class TargetData(
        @SerializedName("name")
        val name: String = "",
        @SerializedName("hide_if_no_complications")
        val hideIfNoComplications: Boolean = false,
        @SerializedName("hide_title_on_aod")
        val hideTitleOnAod: Boolean = false,
        @SerializedName("open_expanded_on_click")
        val openExpandedOnClick: Boolean = false
    )

}