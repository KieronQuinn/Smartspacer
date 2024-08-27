package com.kieronquinn.app.smartspacer.components.smartspace.complications

import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.DigitalWellbeingWidget
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class DigitalWellbeingComplication: SmartspacerComplicationProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.target.digitalwellbeing"
    }

    private val wellbeingRepository by inject<DigitalWellbeingRepository>()

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        wellbeingRepository.addSmartspacerIdIfNeeded(smartspacerId)
        val state = wellbeingRepository.getState() ?: return emptyList()
        return listOf(
            ComplicationTemplate.Basic(
                "digital_wellbeing",
                Icon(AndroidIcon.createWithResource(
                    provideContext(), R.drawable.ic_target_digital_wellbeing
                )),
                Text(state.screenTime),
                TapAction(pendingIntent = state.clickIntent)
            ).create().apply {
                extras.putBoolean(SmartspaceAction.KEY_EXTRA_HIDE_TITLE_ON_AOD, true)
            }
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        wellbeingRepository.removeSmartspacerId(smartspacerId)
    }
    
    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.complication_digital_wellbeing_label),
            description = resources.getString(R.string.complication_digital_wellbeing_description),
            icon = AndroidIcon.createWithResource(
                provideContext(), R.drawable.ic_target_digital_wellbeing
            ),
            compatibilityState = getCompatibility(),
            widgetProvider = "${BuildConfig.APPLICATION_ID}.widget.digitalwellbeing",
            refreshPeriodMinutes = 1,
            allowAddingMoreThanOnce = true
        )
    }

    private fun getCompatibility(): CompatibilityState {
        return if(DigitalWellbeingWidget.getProviderInfo() != null){
            CompatibilityState.Compatible
        }else{
            val unsupported = provideContext().getString(
                R.string.complication_digital_wellbeing_unsupported
            )
            CompatibilityState.Incompatible(unsupported)
        }
    }
    
}