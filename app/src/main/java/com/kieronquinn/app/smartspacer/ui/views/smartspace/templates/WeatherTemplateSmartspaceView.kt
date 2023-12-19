package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import java.util.Locale
import java.util.UUID

/**
 *  Special case: Basic template with WEATHER feature type needs to show a date instead of the
 *  title.
 */
class WeatherTemplateSmartspaceView(
    targetId: String,
    override val target: SmartspaceTarget,
    override val template: BaseTemplateData,
    override val surface: UiSurface
): BaseTemplateSmartspaceView<BaseTemplateData>(targetId, target, template, surface) {

    override val layoutRes = R.layout.smartspace_view_template_weather
    override val viewType = ViewType.TEMPLATE_WEATHER

    private val dateFormat =
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE, MMM d")

    override val supportsSubAction = true

    override fun apply(
        context: Context,
        textColour: Int,
        remoteViews: RemoteViews,
        width: Int,
        titleSize: Float,
        subtitleSize: Float,
        featureSize: Float
    ) {
        super.apply(context, textColour, remoteViews, width, titleSize, subtitleSize, featureSize)
        remoteViews.setCharSequence(R.id.smartspace_view_title, "setFormat12Hour", dateFormat)
        remoteViews.setCharSequence(R.id.smartspace_view_title, "setFormat24Hour", dateFormat)
        val calendarTrampolineIntent = Intent(context, TrampolineActivity::class.java).apply {
            putExtra(TrampolineActivity.EXTRA_LAUNCH_CALENDAR, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            applySecurity(context)
        }.let {
            PendingIntent.getActivity(
                context,
                UUID.randomUUID().hashCode(),
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        remoteViews.setOnClickPendingIntent(R.id.smartspace_view_title, calendarTrampolineIntent)
    }

}