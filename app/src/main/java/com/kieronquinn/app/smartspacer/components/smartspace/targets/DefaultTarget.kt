package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.Context
import android.graphics.drawable.Icon
import androidx.annotation.StringRes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
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

    private fun List<SmartspaceTarget>.filter(settings: TargetData): List<SmartspaceTarget> {
        val hiddenTargetTypes = settings.hiddenTargetTypes.mapNotNull { type ->
            TargetType.entries.firstOrNull { it.type == type }
        }.flatMap { it.additionalTypes.toList() + it.type }
        return filterNot {
            hiddenTargetTypes.contains(it.getTargetType())
        }
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
            configActivity = ConfigurationActivity.createIntent(provideContext(), TARGET_DEFAULT),
            allowAddingMoreThanOnce = true
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

    enum class TargetType(
        @StringRes val title: Int,
        @StringRes val description: Int,
        val type: String,
        vararg val additionalTypes: String
    ) {
        DOORBELL(R.string.target_default_settings_hide_doorbell, R.string.target_default_settings_hide_doorbell_desc, "DOORBELL", "RING"),
        PACKAGE(R.string.target_default_settings_hide_package, R.string.target_default_settings_hide_package_desc, "PACKAGE_DELIVERY"),
        TIMER(R.string.target_default_settings_hide_timer, R.string.target_default_settings_hide_timer_desc, "TIMER_STOPWATCH", "TIMER", "STOPWATCH"),
        BEDTIME(R.string.target_default_settings_hide_bedtime, R.string.target_default_settings_hide_bedtime_desc, "BEDTIME", "BEDTIME_ROUTINE", "WELLBEING_BEDTIME", "SLEEP_SUMMARY"),
        FITNESS(R.string.target_default_settings_hide_fitness, R.string.target_default_settings_hide_fitness_desc, "FITNESS", "FITNESS_TRACKING", "STEP_COUNTING"),
        CONNECTED_DEVICES(R.string.target_default_settings_hide_connected_devices, R.string.target_default_settings_hide_connected_devices_desc, "CONNECTED_DEVICES", "PAIRED_DEVICE_STATUS", "PAIRED_DEVICE_LOW_BATTERY", "HEADPHONE_CONTEXT"),
        FLASHLIGHT(R.string.target_default_settings_hide_flashlight, R.string.target_default_settings_hide_flashlight_desc, "FLASHLIGHT"),
        SAFETY_CHECK(R.string.target_default_settings_hide_safety_check, R.string.target_default_settings_hide_safety_check_desc, "SAFETY_CHECK"),
        EARTHQUAKE_ALERT(R.string.target_default_settings_hide_earthquake_alert, R.string.target_default_settings_hide_earthquake_alert_desc, "EARTHQUAKE", "EARTHQUAKE_OCCURRED"),
        COMMUTE(R.string.target_default_settings_hide_commute, R.string.target_default_settings_hide_commute_desc, "COMMUTE", "COMMUTE_TIME", "COMMUTE_TIME_AMBIENT", "SEMANTIC_LOCATION"),
        TIME_TO_LEAVE(R.string.target_default_settings_hide_time_to_leave, R.string.target_default_settings_hide_time_to_leave_desc, "TIME_TO_LEAVE"),
        WEATHER_ALERTS(R.string.target_default_settings_hide_weather_alerts, R.string.target_default_settings_hide_weather_alerts_desc, "WEATHER_ALERT", "SEVERE_WEATHER_ALERT", "ALERTS"),
        TRAVEL(R.string.target_default_settings_hide_travel, R.string.target_default_settings_hide_travel_desc, "FLIGHT", "FLIGHT_LANDING", "AIRPORT", "TRAVEL"),
        CALENDAR(R.string.target_default_settings_hide_calendar, R.string.target_default_settings_hide_calendar_desc, "CALENDAR", "CALENDAR_NOTIFICATION", "UPCOMING"),
        WORK_PROFILE(R.string.target_default_settings_hide_work_profile, R.string.target_default_settings_hide_work_profile_desc, "WORK_PROFILE"),
        FOOD(R.string.target_default_settings_hide_food, R.string.target_default_settings_hide_food_desc, "FOOD_DELIVERY_ETA", "GROCERY", "GROCERY_DELIVERY", "GROCERY_PICKUP", "RESTAURANT"),
        CROSS_DEVICE_TIMER(R.string.target_default_settings_hide_cross_device_timer, R.string.target_default_settings_cross_device_timer_desc, "CROSS_DEVICE_TIMER"),
        // Weather is intentionally excluded as it wouldn't work with the complication extraction
        AIR_QUALITY(R.string.target_default_settings_hide_air_quality, R.string.target_default_settings_hide_air_quality_desc, "AIR_QUALITY"),
        ALARMS(R.string.target_default_settings_hide_alarms, R.string.target_default_settings_hide_alarms_desc, "ALARM", "UPCOMING_ALARM", "HOLIDAY_ALARMS"),
        CROSS_DEVICE_ALARM(R.string.target_default_settings_hide_cross_device_alarm, R.string.target_default_settings_hide_cross_device_alarm_desc, "CROSS_DEVICE_ALARM"),
        REMINDERS(R.string.target_default_settings_hide_reminders, R.string.target_default_settings_hide_reminders_desc, "REMINDER"),
        SPORTS(R.string.target_default_settings_hide_sports, R.string.target_default_settings_hide_sports_desc, "SPORTS", "SPORTS_SCORES"),
        FINANCE(R.string.target_default_settings_hide_finance, R.string.target_default_settings_hide_finance_desc, "FINANCE_RECAP", "STOCK_PRICE_CHANGE", "STOCK_EARNINGS_CALL"),
        SHOPPING_LIST(R.string.target_default_settings_hide_shopping_list, R.string.target_default_settings_hide_shopping_list_desc, "SHOPPING_LIST", "SHOPPING_LIST_ONBOARDING"),
        WALLET(R.string.target_default_settings_hide_wallet, R.string.target_default_settings_hide_wallet_desc, "WALLET_SUGGESTIONS", "LOYALTY_CARD", "LOYALTY_CARD_ONBOARDING", "WALLET_BOARDING_PASS"),
        MEDIA(R.string.target_default_settings_hide_media, R.string.target_default_settings_hide_media_desc, "MEDIA", "AMBIENT_MUSIC", "MEDIA_RECOMMENDATION", "MEDIA_HEADS_UP", "MEDIA_RECS_DRIVING", "MEDIA_RESUME", "MEDIA_CURRENT_PLAYING", "MEDIA_RESUME_SS_ACTIVATED"),
        IN_STORE(R.string.target_default_settings_hide_in_store, R.string.target_default_settings_hide_in_store_desc, "AT_A_STORE", "AT_STORE_COMBINED_CARD", "SHOPPING_MALL"),
        HOTELS_EVENTS(R.string.target_default_settings_hide_events, R.string.target_default_settings_hide_events_desc, "EVENT_RESERVATION", "HOTEL_CHECK_IN", "HOTEL_CHECK_OUT"),
        TRANSIT(R.string.target_default_settings_hide_transit, R.string.target_default_settings_hide_transit_desc, "TRANSIT_STATION", "TRANSIT_STATION_INFO", "TRAIN_SEAT", "TRAIN_STATUS", "TRAIN_DESTINATION_ALERT", "INTERCITY_TRAIN"),
        RIDESHARING(R.string.target_default_settings_hide_ridesharing, R.string.target_default_settings_hide_ridesharing_desc, "RIDESHARING_ETA", "ETA_MONITORING"),
        DRIVING(R.string.target_default_settings_hide_driving, R.string.target_default_settings_hide_driving_desc, "DRIVING_MODE", "GAS_STATION_PAYMENT"),
        LOUD_SOUND_ALERT(R.string.target_default_settings_hide_loud_sound, R.string.target_default_settings_hide_loud_sound_desc, "LOUD_SOUND_ALERT"),
        PERSONAL(R.string.target_default_settings_hide_personal, R.string.target_default_settings_hide_personal_desc, "BIRTHDAY", "DATE"),
        COMMUNICATION(R.string.target_default_settings_hide_communication, R.string.target_default_settings_hide_communication_desc, "MISSED_CALL"),
        TIPS(R.string.target_default_settings_hide_tips, R.string.target_default_settings_hide_tips_desc, "TIPS", "MY_PIXEL", "ASSISTANT")
    }

}