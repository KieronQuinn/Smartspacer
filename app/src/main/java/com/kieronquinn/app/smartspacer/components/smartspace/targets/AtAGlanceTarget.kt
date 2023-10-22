package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.app.PendingIntent
import android.content.ComponentName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.AtAGlanceWidget
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.GlanceWidget
import com.kieronquinn.app.smartspacer.receivers.AtAGlanceClickReceiver
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import com.kieronquinn.app.smartspacer.utils.extensions.PendingIntent_MUTABLE_FLAGS
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class AtAGlanceTarget: SmartspacerTargetProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.target.ataglance"
    }

    private val atAGlance by inject<AtAGlanceRepository>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val state = atAGlance.getState() ?: return emptyList()
        val click = if(state.clickPendingIntent != null && state.clickIntent != null) {
            PendingIntent.getBroadcast(
                provideContext(),
                smartspacerId.hashCode(),
                AtAGlanceClickReceiver.createIntent(provideContext(), smartspacerId),
                PendingIntent_MUTABLE_FLAGS
            )
        }else state.clickPendingIntent
        return listOf(
            TargetTemplate.Basic(
                "at_a_glance",
                ComponentName(BuildConfig.APPLICATION_ID, "at_a_glance"),
                title = Text(state.title),
                subtitle = Text(state.subtitle),
                icon = Icon(AndroidIcon.createWithBitmap(state.icon)),
                onClick = TapAction(pendingIntent = click)
            ).create()
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        atAGlance.setState(null)
        notifyChange(smartspacerId)
        return true
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            provideContext().getString(R.string.target_at_a_glance_label),
            provideContext().getString(R.string.target_at_a_glance_description),
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_at_a_glance),
            compatibilityState = getCompatibility(),
            widgetProvider = AtAGlanceWidget.AUTHORITY,
            configActivity = TrampolineActivity.createGlanceTrampolineIntent(provideContext())
        )
    }

    private fun getCompatibility(): CompatibilityState {
        return if(GlanceWidget.getProviderInfo() != null){
            CompatibilityState.Compatible
        }else{
            val unsupported = provideContext().getString(
                R.string.target_at_a_glance_description_unsupported
            )
            CompatibilityState.Incompatible(unsupported)
        }
    }

}