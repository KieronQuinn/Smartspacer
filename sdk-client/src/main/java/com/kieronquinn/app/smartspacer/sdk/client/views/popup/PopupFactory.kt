package com.kieronquinn.app.smartspacer.sdk.client.views.popup

import android.content.Context
import android.content.Intent
import android.view.View
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget

interface PopupFactory {

    fun createPopup(
        context: Context,
        anchorView: View,
        target: SmartspaceTarget,
        backgroundColor: Int,
        textColour: Int,
        launchIntent: (Intent?) -> Unit,
        dismissAction: ((SmartspaceTarget) -> Unit)? = null,
        aboutIntent: Intent?,
        feedbackIntent: Intent?,
        settingsIntent: Intent?
    ): Popup

}