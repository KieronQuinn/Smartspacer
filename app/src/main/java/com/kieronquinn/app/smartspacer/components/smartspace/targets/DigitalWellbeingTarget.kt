package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.DigitalWellbeingWidget
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository.WellbeingState
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.utils.extensions.Icon_createEmptyIcon
import com.kieronquinn.app.smartspacer.utils.extensions.takeEllipsised
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class DigitalWellbeingTarget: SmartspacerTargetProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.target.digitalwellbeing"
    }

    private val wellbeingRepository by inject<DigitalWellbeingRepository>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        wellbeingRepository.addSmartspacerIdIfNeeded(smartspacerId)
        val state = wellbeingRepository.getState() ?: return emptyList()
        return listOf(createTarget(state))
    }

    private fun createTarget(state: WellbeingState): SmartspaceTarget {
        val maxLength = 15
        val app1 = if(state.app1Name.isNotEmpty()){
            "${state.app1Time}: ${state.app1Name}"
        }else null
        val app2 = if(state.app2Name.isNotEmpty()){
            "${state.app2Time}: ${state.app2Name}"
        }else null
        val app3 = if(state.app3Name.isNotEmpty()){
            "${state.app2Time}: ${state.app3Name}"
        }else null
        val items = listOfNotNull(
            app1?.let { Text(it.takeEllipsised(maxLength)) },
            app2?.let { Text(it.takeEllipsised(maxLength)) },
            app3?.let { Text(it.takeEllipsised(maxLength)) },
        )
        return if(items.isNotEmpty()){
            TargetTemplate.ListItems(
                "digital_wellbeing",
                ComponentName(provideContext(), this::class.java),
                provideContext(),
                Text("${state.title}: ${state.screenTime}"),
                Text(resources.getString(R.string.target_digital_wellbeing_label)),
                Icon(AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_digital_wellbeing)),
                items,
                Icon(Icon_createEmptyIcon()),
                Text(""), //Never empty
                TapAction(pendingIntent = state.clickIntent)
            ).create()
        }else{
            TargetTemplate.Basic(
                "digital_wellbeing",
                ComponentName(provideContext(), this::class.java),
                title = Text("${state.title}: ${state.screenTime}"),
                subtitle = Text(resources.getString(R.string.target_digital_wellbeing_label)),
                icon = Icon(AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_digital_wellbeing)),
                onClick = TapAction(pendingIntent = state.clickIntent)
            ).create()
        }.also {
            it.canBeDismissed = false
        }
    }

    override fun onProviderRemoved(smartspacerId: String) {
        super.onProviderRemoved(smartspacerId)
        wellbeingRepository.removeSmartspacerId(smartspacerId)
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.target_digital_wellbeing_label),
            description = resources.getString(R.string.target_digital_wellbeing_description),
            icon = AndroidIcon.createWithResource(
                provideContext(), R.drawable.ic_target_digital_wellbeing
            ),
            compatibilityState = getCompatibility(),
            widgetProvider = "${BuildConfig.APPLICATION_ID}.widget.digitalwellbeing",
            refreshPeriodMinutes = 1
        )
    }

    private fun getCompatibility(): CompatibilityState {
        return if(DigitalWellbeingWidget.getProviderInfo() != null){
            CompatibilityState.Compatible
        }else{
            val unsupported = provideContext().getString(
                R.string.target_digital_wellbeing_unsupported
            )
            CompatibilityState.Incompatible(unsupported)
        }
    }

    //Can't dismiss this target
    override fun onDismiss(smartspacerId: String, targetId: String) = false

}