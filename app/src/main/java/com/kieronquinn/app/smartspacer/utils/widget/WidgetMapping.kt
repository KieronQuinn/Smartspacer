package com.kieronquinn.app.smartspacer.utils.widget

import android.content.ComponentName
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kieronquinn.app.smartspacer.R

/**
 *  Replicates custom mapping of widgets from the Pixel Launcher
 */
object WidgetMapping {

    private val MAPPING = listOf(
        WidgetMap(
            "\$people",
            ComponentName.unflattenFromString(
                "com.android.systemui/.people.widget.PeopleSpaceWidgetProvider"
            )!!,
            R.drawable.ic_conversations_widget_category,
            R.string.expanded_add_widget_category_conversations
        ),
        WidgetMap(
            "\$notes",
            ComponentName.unflattenFromString(
                "com.android.systemui/com.android.systemui.notetask.shortcut.CreateNoteTaskShortcutActivity"
            )!!,
            R.drawable.ic_note_taking_widget_category,
            R.string.expanded_add_widget_category_note_taking
        ),
        WidgetMap(
            "\$weather",
            ComponentName.unflattenFromString(
                "com.google.android.googlequicksearchbox/com.google.android.apps.search.assistant.verticals.snapshot.widgets.weather.WeatherWidget_Receiver"
            )!!,
            R.drawable.ic_weather_widget_category,
            R.string.expanded_add_widget_category_weather
        ),
        WidgetMap(
            "\$weather",
            ComponentName.unflattenFromString(
                "com.google.android.googlequicksearchbox/com.google.android.apps.search.assistant.verticals.snapshot.widgets.weather.FreeformWeatherWidget_Receiver"
            )!!,
            R.drawable.ic_weather_widget_category,
            R.string.expanded_add_widget_category_weather
        ),
        WidgetMap(
            "\$battery",
            ComponentName.unflattenFromString(
                "com.google.android.settings.intelligence/com.google.android.settings.intelligence.modules.batterywidget.impl.BatteryAppWidgetProvider"
            )!!,
            R.drawable.ic_battery_widget_category,
            R.string.expanded_add_widget_category_battery
        )
    )

    fun getWidgetMapping(provider: ComponentName): WidgetMap? {
        return MAPPING.firstOrNull { it.provider == provider }
    }

    data class WidgetMap(
        val identifier: String,
        val provider: ComponentName,
        @DrawableRes
        val icon: Int,
        @StringRes
        val label: Int
    )

}