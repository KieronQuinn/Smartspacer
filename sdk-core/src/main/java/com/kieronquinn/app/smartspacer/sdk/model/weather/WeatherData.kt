package com.kieronquinn.app.smartspacer.sdk.model.weather

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf

/**
 *  Weather Data for Android 14 AoD and lock screen. Matches the system one from SystemUI.
 */
data class WeatherData(
    /**
     *  Content description for weather icon
     */
    val description: String,
    /**
     *  Weather icon - this must already be in the system and cannot be custom
     */
    val state: WeatherStateIcon,
    /**
     *  Whether to use Celsius. Consider using LocalePreferences.getTemperatureUnit() if your
     *  source does not provide a setting to change this.
     */
    val useCelsius: Boolean,
    /**
     *  The weather temperature, without a unit
     */
    val temperature: Int
) {

    companion object {
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_STATE = "state"
        private const val KEY_USE_CELSIUS = "use_celsius"
        private const val KEY_TEMPERATURE = "temperature"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun clearExtras(bundle: Bundle) {
            bundle.remove(KEY_DESCRIPTION)
            bundle.remove(KEY_STATE)
            bundle.remove(KEY_USE_CELSIUS)
            bundle.remove(KEY_TEMPERATURE)
        }

        fun fromBundle(bundle: Bundle): WeatherData? = with(bundle) {
            if(!containsKey(KEY_DESCRIPTION)) return@with null
            if(!containsKey(KEY_STATE)) return@with null
            if(!containsKey(KEY_USE_CELSIUS)) return@with null
            if(!containsKey(KEY_TEMPERATURE)) return@with null
            WeatherData(this)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getString(KEY_DESCRIPTION)!!,
        bundle.getInt(KEY_STATE, 0).let { WeatherStateIcon.fromId(it) },
        bundle.getBoolean(KEY_USE_CELSIUS),
        bundle.getString(KEY_TEMPERATURE)!!.toInt()
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle(): Bundle {
        return bundleOf(
            KEY_DESCRIPTION to description,
            KEY_STATE to state.id,
            KEY_USE_CELSIUS to useCelsius,
            KEY_TEMPERATURE to temperature.toString()
        )
    }

    enum class WeatherStateIcon(
        val id: Int
    ) {
        UNKNOWN_ICON(0),
        SUNNY(1),
        CLEAR_NIGHT(2),
        MOSTLY_SUNNY(3),
        MOSTLY_CLEAR_NIGHT(4),
        PARTLY_CLOUDY(5),
        PARTLY_CLOUDY_NIGHT(6),
        MOSTLY_CLOUDY_DAY(7),
        MOSTLY_CLOUDY_NIGHT(8),
        CLOUDY(9),
        HAZE_FOG_DUST_SMOKE(10),
        DRIZZLE(11),
        HEAVY_RAIN(12),
        SHOWERS_RAIN(13),
        SCATTERED_SHOWERS_DAY(14),
        SCATTERED_SHOWERS_NIGHT(15),
        ISOLATED_SCATTERED_TSTORMS_DAY(16),
        ISOLATED_SCATTERED_TSTORMS_NIGHT(17),
        STRONG_TSTORMS(18),
        BLIZZARD(19),
        BLOWING_SNOW(20),
        FLURRIES(21),
        HEAVY_SNOW(22),
        SCATTERED_SNOW_SHOWERS_DAY(23),
        SCATTERED_SNOW_SHOWERS_NIGHT(24),
        SNOW_SHOWERS_SNOW(25),
        MIXED_RAIN_HAIL_RAIN_SLEET(26),
        SLEET_HAIL(27),
        TORNADO(28),
        TROPICAL_STORM_HURRICANE(29),
        WINDY_BREEZY(30),
        WINTRY_MIX_RAIN_SNOW(31);

        companion object {
            fun fromId(id: Int): WeatherStateIcon {
                return values().firstOrNull { it.id == id } ?: UNKNOWN_ICON
            }
        }
    }

}