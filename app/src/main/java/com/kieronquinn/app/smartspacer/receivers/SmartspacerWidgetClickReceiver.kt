package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.sdk.utils.sendSafely
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import com.kieronquinn.app.smartspacer.utils.extensions.getSerializableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.isValid
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartspacerWidgetClickReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        private const val KEY_TARGET_ID = "target_id"
        private const val KEY_SMARTSPACE_ACTION = "smartspace_action"
        private const val KEY_TAP_ACTION = "tap_action"
        private const val KEY_SURFACE = "surface"

        fun createIntent(
            context: Context,
            targetId: String,
            surface: UiSurface,
            smartspaceAction: SmartspaceAction? = null,
            tapAction: TapAction? = null
        ): Intent {
            return Intent(context, SmartspacerWidgetClickReceiver::class.java).apply {
                applySecurity(context)
                putExtra(KEY_SMARTSPACE_ACTION, smartspaceAction?.toBundle())
                putExtra(KEY_TAP_ACTION, tapAction?.toBundle())
                putExtra(KEY_SURFACE, surface)
                putExtra(KEY_TARGET_ID, targetId)
            }
        }
    }

    private val smartspaceRepository by inject<SmartspaceRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        intent.verifySecurity()
        val targetId = intent.getStringExtra(KEY_TARGET_ID) ?: return
        val smartspaceAction = intent.getBundleExtra(KEY_SMARTSPACE_ACTION)?.let {
            SmartspaceAction(it)
        }
        val tapAction = intent.getBundleExtra(KEY_TAP_ACTION)?.let {
            TapAction(it)
        }
        val id = tapAction?.id?.toString() ?: smartspaceAction?.id
        val surface = intent.getSerializableExtraCompat(KEY_SURFACE, UiSurface::class.java)
            ?: return
        when {
            smartspaceAction != null -> smartspaceAction.handleSmartspaceAction(context, surface)
            tapAction != null -> tapAction.handleTapAction(context, surface)
        }
        smartspaceRepository.notifyClickEvent(targetId, id)
    }

    private fun TapAction.handleTapAction(context: Context, surface: UiSurface) {
        when {
            intent.isValid(context) -> {
                if(surface == UiSurface.HOMESCREEN || shouldShowOnLockScreen) {
                    //We can launch this directly
                    context.startActivity(intent?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }else{
                    //This needs to be proxied
                    val trampolineIntent = TrampolineActivity.createTrampolineIntent(
                        context,
                        intent = intent,
                        dismissLockScreen = !shouldShowOnLockScreen
                    )
                    context.startActivity(trampolineIntent)
                }
            }
            pendingIntent != null -> {
                if(surface == UiSurface.HOMESCREEN || shouldShowOnLockScreen) {
                    //We can launch this directly
                    pendingIntent?.sendSafely()
                }else{
                    //This needs to be proxied
                    val trampolineIntent = TrampolineActivity.createTrampolineIntent(
                        context,
                        pendingIntent = pendingIntent,
                        dismissLockScreen = !shouldShowOnLockScreen
                    )
                    context.startActivity(trampolineIntent)
                }
            }
        }
    }

    private fun SmartspaceAction.handleSmartspaceAction(context: Context, surface: UiSurface) {
        when {
            intent.isValid(context) -> {
                if(surface == UiSurface.HOMESCREEN || launchDisplayOnLockScreen) {
                    context.startActivity(intent?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }else{
                    //This needs to be proxied
                    val trampolineIntent = TrampolineActivity.createTrampolineIntent(
                        context,
                        intent = intent,
                        dismissLockScreen = !launchDisplayOnLockScreen
                    )
                    context.startActivity(trampolineIntent)
                }
            }
            pendingIntent != null -> {
                if(surface == UiSurface.HOMESCREEN || launchDisplayOnLockScreen) {
                    //We can launch this directly
                    pendingIntent?.sendSafely()
                }else{
                    //This needs to be proxied
                    val trampolineIntent = TrampolineActivity.createTrampolineIntent(
                        context,
                        pendingIntent = pendingIntent,
                        dismissLockScreen = !launchDisplayOnLockScreen
                    )
                    context.startActivity(trampolineIntent)
                }
            }
        }
    }

}