package com.kieronquinn.app.smartspacer.repositories

import android.app.PendingIntent
import android.content.Context
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DigitalWellbeingComplication
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DigitalWellbeingTarget
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository.WellbeingState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import org.jetbrains.annotations.VisibleForTesting

interface DigitalWellbeingRepository {

    fun addSmartspacerIdIfNeeded(id: String)
    fun removeSmartspacerId(id: String)
    fun getState(): WellbeingState?
    fun setState(wellbeingState: WellbeingState)
    fun refreshWidgetIfNeeded(id: String)

    data class WellbeingState(
        val title: CharSequence,
        val screenTime: CharSequence,
        val app1Name: CharSequence,
        val app1Time: CharSequence,
        val app2Name: CharSequence,
        val app2Time: CharSequence,
        val app3Name: CharSequence,
        val app3Time: CharSequence,
        val clickIntent: PendingIntent
    )

}

class DigitalWellbeingRepositoryImpl(
    private val context: Context
): DigitalWellbeingRepository {

    private var state: WellbeingState? = null

    /**
     *  Local in-memory set of Target & Complication ids. This is used to know whether to trigger a
     *  refresh when Smartspace becomes visible.
     */
    @VisibleForTesting
    val ids = HashSet<String>()

    override fun getState(): WellbeingState? {
        return state
    }

    override fun setState(wellbeingState: WellbeingState) {
        state = wellbeingState
        SmartspacerTargetProvider.notifyChange(context, DigitalWellbeingTarget::class.java)
        SmartspacerComplicationProvider.notifyChange(
            context, DigitalWellbeingComplication::class.java
        )
    }

    override fun addSmartspacerIdIfNeeded(id: String) {
        if(ids.add(id)){
            refreshWidgetIfNeeded(id)
        }
    }

    override fun removeSmartspacerId(id: String) {
        ids.remove(id)
    }

    override fun refreshWidgetIfNeeded(id: String) {
        if(!ids.contains(id)) return
        SmartspacerWidgetProvider.clickView(
            context, id, "com.google.android.apps.wellbeing:id/screen_time_refresh_view"
        )
    }

}