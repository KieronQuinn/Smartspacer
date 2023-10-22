package com.kieronquinn.app.smartspacer.sdk.client.views.popup

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.sdk.client.R
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspaceLongPressPopupBinding
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec

/**
 *  Default Popup Factory, uses [Balloon]. Since this is an external dependency and may not work
 *  for all clients, you can create your own factory with your own popup implementation if you
 *  desire.
 */
object BalloonPopupFactory: PopupFactory {

    override fun createPopup(
        context: Context,
        anchorView: View,
        target: SmartspaceTarget,
        backgroundColor: Int,
        textColour: Int,
        launchIntent: (Intent?) -> Unit,
        dismissAction: ((SmartspaceTarget) -> Unit)?,
        aboutIntent: Intent?,
        feedbackIntent: Intent?,
        settingsIntent: Intent?
    ): Popup {
        val layoutInflater = LayoutInflater.from(context)
        val popupView = SmartspaceLongPressPopupBinding.inflate(layoutInflater)
        val popup = Balloon.Builder(context)
            .setLayout(popupView)
            .setHeight(BalloonSizeSpec.WRAP)
            .setWidthResource(R.dimen.smartspace_long_press_popup_width)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setBackgroundColor(backgroundColor)
            .setArrowColor(backgroundColor)
            .setArrowSize(10)
            .setArrowPosition(0.5f)
            .setCornerRadius(16f)
            .setBalloonAnimation(BalloonAnimation.FADE)
            .build()
        popup.showAlignBottom(anchorView)
        popupView.smartspaceLongPressPopupDismiss.isVisible = dismissAction != null
        popupView.smartspaceLongPressPopupDismiss.setTextColor(textColour)
        popupView.smartspaceLongPressPopupDismiss.compoundDrawableTintList =
            ColorStateList.valueOf(textColour)
        popupView.smartspaceLongPressPopupDismiss.setOnClickListener {
            popup.dismiss()
            dismissAction?.invoke(target)
        }
        popupView.smartspaceLongPressPopupAbout.isVisible = aboutIntent != null
        popupView.smartspaceLongPressPopupAbout.setTextColor(textColour)
        popupView.smartspaceLongPressPopupAbout.compoundDrawableTintList =
            ColorStateList.valueOf(textColour)
        popupView.smartspaceLongPressPopupAbout.setOnClickListener {
            popup.dismiss()
            launchIntent(aboutIntent)
        }
        popupView.smartspaceLongPressPopupFeedback.isVisible = feedbackIntent != null
        popupView.smartspaceLongPressPopupFeedback.setTextColor(textColour)
        popupView.smartspaceLongPressPopupFeedback.compoundDrawableTintList =
            ColorStateList.valueOf(textColour)
        popupView.smartspaceLongPressPopupFeedback.setOnClickListener {
            popup.dismiss()
            launchIntent(feedbackIntent)
        }
        popupView.smartspaceLongPressPopupSettings.isVisible = settingsIntent != null
        popupView.smartspaceLongPressPopupSettings.setTextColor(textColour)
        popupView.smartspaceLongPressPopupSettings.compoundDrawableTintList =
            ColorStateList.valueOf(textColour)
        popupView.smartspaceLongPressPopupSettings.setOnClickListener {
            popup.dismiss()
            launchIntent(settingsIntent)
        }
        return BalloonWrapper(popup)
    }

    class BalloonWrapper(private var popupView: Balloon?): Popup {

        override fun dismiss() {
            popupView?.dismiss()
            popupView = null
        }

    }

}