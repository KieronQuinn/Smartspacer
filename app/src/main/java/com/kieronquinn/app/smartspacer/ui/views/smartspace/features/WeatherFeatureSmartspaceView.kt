package com.kieronquinn.app.smartspacer.ui.views.smartspace.features

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import java.util.Locale

class WeatherFeatureSmartspaceView(
    private val targetId: String,
    target: SmartspaceTarget,
    surface: UiSurface
): BaseFeatureSmartspaceView(targetId, target, surface) {

    override val layoutRes = R.layout.smartspace_view_feature_weather
    override val viewType = ViewType.FEATURE_WEATHER

    private val dateFormat =
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE, MMM d")

    override val supportsSubAction = true

    override fun apply(
        context: Context,
        textColour: Int,
        shadowEnabled: Boolean,
        remoteViews: RemoteViews,
        width: Int,
        titleSize: Float,
        subtitleSize: Float,
        featureSize: Float,
        isList: Boolean,
        overflowIntent: Intent?
    ) {
        super.apply(
            context,
            textColour,
            shadowEnabled,
            remoteViews,
            width,
            titleSize,
            subtitleSize,
            featureSize,
            isList,
            overflowIntent,
        )
        remoteViews.setCharSequence(R.id.smartspace_view_title, "setFormat12Hour", dateFormat)
        remoteViews.setCharSequence(R.id.smartspace_view_title, "setFormat24Hour", dateFormat)
        val calendarTrampolineIntent = Intent(context, TrampolineActivity::class.java).apply {
            putExtra(TrampolineActivity.EXTRA_LAUNCH_CALENDAR, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            applySecurity(context)
        }
        val tapAction = TapAction(
            intent = calendarTrampolineIntent,
            shouldShowOnLockScreen = false
        )
        remoteViews.setOnClickAction(
            context,
            R.id.smartspace_view_title,
            targetId,
            surface,
            tapAction,
            isList
        )
    }

}