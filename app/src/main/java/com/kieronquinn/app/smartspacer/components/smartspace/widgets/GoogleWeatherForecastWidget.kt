package com.kieronquinn.app.smartspacer.components.smartspace.widgets

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import android.util.SizeF
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.view.children
import com.kieronquinn.app.smartspacer.components.smartspace.targets.GoogleWeatherForecastTarget
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.ForecastState
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.ForecastState.Loaded.ForecastItem
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.findByType
import com.kieronquinn.app.smartspacer.sdk.utils.getClickPendingIntent
import com.kieronquinn.app.smartspacer.sdk.utils.viewstructure.ViewGroup
import com.kieronquinn.app.smartspacer.sdk.utils.viewstructure.mapWidgetViewStructure
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import org.koin.android.ext.android.inject

class GoogleWeatherForecastWidget: SmartspacerWidgetProvider() {

    companion object {
        private const val PACKAGE_NAME = "com.google.android.apps.weather"
        private const val CLASS = "com.google.android.apps.weather.widget.FullForecastWidgetReceiver"

        private const val IDENTIFIER_LOCATION = "location_title"
        private const val IDENTIFIER_CONDITION = "condition_title"
        private const val IDENTIFIER_ICON = "weather_icon"
        private const val IDENTIFIER_TEMPERATURE = "temperature"
        private const val IDENTIFIER_FORECASTS = "forecasts"
        private const val IDENTIFIER_ERROR_TITLE = "error_title"
        private const val IDENTIFIER_ERROR_SUBTITLE = "error_subtitle"
        private const val IDENTIFIER_ERROR_ICON = "error_icon"
        private const val IDENTIFIER_ERROR_CLICKABLE = "error_clickable"

        private val SMALL_ICON_SIZE = 24.dp
        private val WIDGET_WIDTH = 368.dp
        private val WIDGET_HEIGHT = 146.dp

        fun getProviderInfo(widgetRepository: WidgetRepository): AppWidgetProviderInfo? {
            return widgetRepository.getProviders().firstOrNull {
                it.provider.packageName == PACKAGE_NAME && it.provider.className == CLASS
            }
        }

        private val STRUCTURE_LOADED: ViewGroup.() -> Unit = {
            frameLayout {
                frameLayout {
                    linearLayout {
                        frameLayout {
                            linearLayout {
                                linearLayout {
                                    textView {
                                        index = 0
                                        id = IDENTIFIER_LOCATION
                                    }
                                    linearLayout {
                                        index = 2
                                        linearLayout {
                                            index = 0
                                            linearLayout {
                                                imageView {
                                                    index = 0
                                                    id = IDENTIFIER_ICON
                                                }
                                                textView {
                                                    index = 2
                                                    id = IDENTIFIER_CONDITION
                                                }
                                            }
                                        }
                                        textView {
                                            index = 2
                                            id = IDENTIFIER_TEMPERATURE
                                        }
                                    }
                                    linearLayout {
                                        index = 4
                                        id = IDENTIFIER_FORECASTS
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private val STRUCTURE_ERROR: ViewGroup.() -> Unit = {
            frameLayout {
                linearLayout {
                    frameLayout {
                        frameLayout {
                            frameLayout {
                                frameLayout {
                                    linearLayout {
                                        imageView {
                                            id = IDENTIFIER_ERROR_ICON
                                        }
                                        linearLayout {
                                            textView {
                                                index = 1
                                                id = IDENTIFIER_ERROR_TITLE
                                            }
                                        }
                                        frameLayout {
                                            id = IDENTIFIER_ERROR_CLICKABLE
                                            frameLayout {
                                                index = 1
                                                id = IDENTIFIER_ERROR_SUBTITLE
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val widgetRepository by inject<WidgetRepository>()
    private val googleWeatherRepository by inject<GoogleWeatherRepository>()

    override fun onWidgetChanged(smartspacerId: String, remoteViews: RemoteViews?) {
        val fullSize = SizeF(WIDGET_WIDTH.toFloat(), WIDGET_HEIGHT.toFloat())
        val fullWidthRemoteViews = getSizedRemoteView(remoteViews ?: return, fullSize)
        val views = fullWidthRemoteViews?.load() ?: return
        val isError = views.findViewById<View>(android.R.id.background) is LinearLayout
        val forecastState = if (isError) {
            mapWidgetViewStructure(views, STRUCTURE_ERROR)?.loadErrorState(views)
        } else {
            mapWidgetViewStructure(views, STRUCTURE_LOADED)?.loadLoadedState(views)
        }
        googleWeatherRepository.setForecastState(forecastState)
        SmartspacerTargetProvider.notifyChange(
            provideContext(), GoogleWeatherForecastTarget::class.java
        )
    }

    private fun ViewGroup.loadLoadedState(views: View): ForecastState.Loaded? {
        val location = findViewByStructureId<TextView>(views, IDENTIFIER_LOCATION)
            ?.text?.toString() ?: return null
        val condition = findViewByStructureId<TextView>(views, IDENTIFIER_CONDITION)
            ?.text?.toString() ?: return null
        val icon = findViewByStructureId<ImageView>(views, IDENTIFIER_ICON)
            ?.getImageAsBitmap() ?: return null
        val temperature =
            findViewByStructureId<TextView>(views, IDENTIFIER_TEMPERATURE)
            ?.text?.toString() ?: return null
        val forecasts = ArrayList<ForecastItem>()
        val forecastContainer =
            findViewByStructureId<LinearLayout>(views, IDENTIFIER_FORECASTS)
            ?.children?.toList() ?: return null
        for(i in 1 until 5) {
            val forecastItem = forecastContainer.getOrNull(i) as? LinearLayout ?: break
            val forecastTemperature = (forecastItem.getChildAt(0) as TextView).text
                ?.toString() ?: continue
            val forecastIcon = (forecastItem.getChildAt(2) as ImageView)
                .getImageAsBitmap()?.scale() ?: continue
            val forecastTime = (forecastItem.getChildAt(4) as TextView).text
                ?.toString() ?: continue
            forecasts.add(ForecastItem(forecastTime, forecastTemperature, forecastIcon))
        }
        val clickIntent = views.findViewById<View>(android.R.id.background)
            ?.getClickPendingIntent()
        return ForecastState.Loaded(
            location, condition, ForecastItem(null, temperature, icon), forecasts, clickIntent
        )
    }

    private fun ViewGroup.loadErrorState(views: View): ForecastState.Error? {
        val title = findViewByStructureId<TextView>(views, IDENTIFIER_ERROR_TITLE)
            ?.text?.toString() ?: return null
        val subtitle = findViewByStructureId<FrameLayout>(views, IDENTIFIER_ERROR_SUBTITLE)
            ?.findByType<TextView>()?.text?.toString() ?: return null
        val icon = findViewByStructureId<ImageView>(views, IDENTIFIER_ERROR_ICON)
            ?.getImageAsBitmap() ?: return null
        val clickIntent = findViewByStructureId<FrameLayout>(
            views, IDENTIFIER_ERROR_CLICKABLE
        )?.getClickPendingIntent() ?: return null
        return ForecastState.Error(title, subtitle, icon, clickIntent)
    }

    private fun Bitmap.scale(): Bitmap {
        return scale(SMALL_ICON_SIZE, SMALL_ICON_SIZE)
    }

    override fun getAppWidgetProviderInfo(smartspacerId: String): AppWidgetProviderInfo? {
        return getProviderInfo(widgetRepository)
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(WIDGET_WIDTH, WIDGET_HEIGHT)
    }

    private fun ImageView.getImageAsBitmap(): Bitmap? {
        return drawable?.toBitmap()
    }

}