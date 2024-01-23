package com.kieronquinn.app.smartspacer.ui.activities.configuration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.NavigationRes
import androidx.core.view.WindowCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.monetcompat.app.MonetCompatActivity

class ConfigurationActivity: MonetCompatActivity() {

    companion object {
        fun createIntent(context: Context, mapping: NavGraphMapping): Intent {
            return Intent().apply {
                applySecurity(context)
                component = ComponentName(
                    context.packageName, "${context.packageName}${mapping.className}"
                )
            }
        }

        fun getNavGraph(activity: ConfigurationActivity): NavGraphMapping? {
            val className = activity.intent.component?.shortClassName ?: return null
            return NavGraphMapping.values().firstOrNull {
                it.className == className
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapping = getNavGraph(this)
        //Widget does not have security verified
        if(mapping != NavGraphMapping.WIDGET_SMARTSPACER) {
            intent.verifySecurity()
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        whenCreated {
            monet.awaitMonetReady()
            setContentView(R.layout.activity_configuration)
        }
    }

    //Mapping of activity-aliases to their respective Nav Graph resources
    enum class NavGraphMapping(val className: String, @NavigationRes val graph: Int) {
        TARGET_CALENDAR(
            ".ui.activities.configuration.target.calendar.CalendarTargetConfigurationActivity",
            R.navigation.nav_graph_configure_target_calendar
        ),
        TARGET_DEFAULT(
            ".ui.activities.configuration.target.default.DefaultTargetConfigurationActivity",
            R.navigation.nav_graph_configure_target_default
        ),
        TARGET_GREETING(
            ".ui.activities.configuration.target.GreetingTargetConfigurationActivity",
            R.navigation.nav_graph_configure_target_greeting
        ),
        TARGET_MUSIC(
            ".ui.activities.configuration.target.music.MusicTargetConfigurationActivity",
            R.navigation.nav_graph_configure_target_music
        ),
        TARGET_NOTIFICATION(
            ".ui.activities.configuration.target.notification.NotificationTargetConfigurationActivity",
            R.navigation.nav_graph_configure_target_notification,
        ),
        TARGET_BLANK(
            ".ui.activities.configuration.target.blank.BlankTargetConfigurationActivity",
            R.navigation.nav_graph_configure_target_blank
        ),
        TARGET_DATE(
            ".ui.activities.configuration.target.date.DateTargetConfigurationActivity",
            R.navigation.nav_graph_configure_target_date
        ),
        TARGET_FLASHLIGHT(
            ".ui.activities.configuration.target.flashlight.FlashlightTargetConfigurationActivity",
            R.navigation.nav_graph_configure_target_flashlight
        ),
        COMPLICATION_GMAIL(
            ".ui.activities.configuration.complication.GmailComplicationConfigurationActivity",
            R.navigation.nav_graph_configure_complication_gmail
        ),
        REQUIREMENT_APP_PREDICTION(
            ".ui.activities.configuration.requirement.appprediction.AppPredictionRequirementConfigurationActivity",
            R.navigation.nav_graph_configure_requirement_app_prediction
        ),
        REQUIREMENT_GEOFENCE(
            ".ui.activities.configuration.requirement.geofence.GeofenceRequirementConfigurationActivity",
            R.navigation.nav_graph_configure_requirement_geofence
        ),
        REQUIREMENT_RECENT_TASK(
            ".ui.activities.configuration.requirement.recenttask.RecentTaskRequirementConfigurationActivity",
            R.navigation.nav_graph_configure_requirement_recent_task
        ),
        REQUIREMENT_TIME_DATE(
            ".ui.activities.configuration.requirement.timedate.TimeDateRequirementConfigurationActivity",
            R.navigation.nav_graph_configure_requirement_time_date
        ),
        REQUIREMENT_WIFI(
            ".ui.activities.configuration.requirement.wifi.WiFiRequirementConfigurationActivity",
            R.navigation.nav_graph_configure_requirement_wifi
        ),
        REQUIREMENT_BLUETOOTH(
            ".ui.activities.configuration.requirement.bluetooth.BluetoothRequirementConfigurationActivity",
            R.navigation.nav_graph_configure_requirement_bluetooth
        ),
        WIDGET_SMARTSPACER(
            ".ui.activities.configuration.appwidget.SmartspacerAppWidgetConfigureActivity",
            R.navigation.nav_graph_configure_widget
        ),
        NATIVE_RECONNECT(
            ".ui.activities.NativeReconnectActivity",
            R.navigation.nav_graph_native_reconnect
        ),
    }

}