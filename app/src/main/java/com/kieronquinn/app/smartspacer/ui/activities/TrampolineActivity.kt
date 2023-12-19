package com.kieronquinn.app.smartspacer.ui.activities

import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardDismissCallback
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.components.smartspace.targets.AsNowPlayingTarget
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.GlanceWidget
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.sdk.utils.sendSafely
import com.kieronquinn.app.smartspacer.ui.views.smartspace.templates.BaseTemplateSmartspaceView.Companion.EXTRA_INTENT
import com.kieronquinn.app.smartspacer.ui.views.smartspace.templates.BaseTemplateSmartspaceView.Companion.EXTRA_PENDING_INTENT
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.resolveActivityCompat
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TrampolineActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_LAUNCH_CALENDAR = "launch_calendar"
        private const val EXTRA_DISMISS_LOCKSCREEN = "dismiss_lockscreen"
        private const val EXTRA_TOKEN = "token"
        private const val EXTRA_TARGET_ID = "target_id"
        private const val EXTRA_ACTION_ID = "action_id"
        private const val EXTRA_URI_INTENT = "uri_intent"

        /**
         *  Checks if the ASI settings intent is available, and if it is then returns the trampoline
         *  to launch the settings.
         */
        fun createAsiTrampolineIntent(context: Context): Intent? {
            val asiIntent = Intent("android.settings.ASI_SMARTSPACE_SETTINGS").apply {
                `package` = AsNowPlayingTarget.PACKAGE_NAME
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if(context.packageManager.resolveActivityCompat(asiIntent) == null) return null
            val component = ComponentName(
                context,
                "${context.packageName}.ui.activities.configuration.target.default.AsiSettingsTrampolineActivity"
            )
            return Intent().apply {
                applySecurity(context)
                setComponent(component)
                putExtra(EXTRA_INTENT, asiIntent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }

        /**
         *  Checks if the ASI settings intent is available, and if it is then returns the trampoline
         *  to launch the settings.
         */
        fun createGlanceTrampolineIntent(context: Context): Intent? {
            val glanceIntent = Intent(
                "com.google.android.apps.gsa.smartspace.SETTINGS"
            ).apply {
                `package` = GlanceWidget.PACKAGE_NAME
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if(context.packageManager.resolveActivityCompat(glanceIntent) == null) return null
            val component = ComponentName(
                context,
                "${context.packageName}.ui.activities.configuration.target.glance.GlanceSettingsTrampolineActivity"
            )
            return Intent().apply {
                applySecurity(context)
                setComponent(component)
                putExtra(EXTRA_INTENT, glanceIntent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }

        fun createTrampolineIntent(
            context: Context,
            intent: Intent? = null,
            pendingIntent: PendingIntent? = null,
            dismissLockScreen: Boolean = true
        ): Intent {
            return Intent(context, TrampolineActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                applySecurity(context)
                intent?.let {
                    putExtra(EXTRA_INTENT, it)
                }
                pendingIntent?.let {
                    putExtra(EXTRA_PENDING_INTENT, it)
                }
                putExtra(EXTRA_DISMISS_LOCKSCREEN, dismissLockScreen)
            }
        }

        fun createUriTrampolineIntent(
            context: Context,
            token: String,
            targetId: String? = null,
            actionId: String? = null,
            uriIntent: String? = null
        ): Intent {
            return Intent(context, TrampolineActivity::class.java).apply {
                putExtra(EXTRA_TOKEN, token)
                targetId?.let {
                    putExtra(EXTRA_TARGET_ID, it)
                }
                actionId?.let {
                    putExtra(EXTRA_ACTION_ID, it)
                }
                uriIntent?.let {
                    putExtra(EXTRA_URI_INTENT, it)
                }
                putExtra(EXTRA_DISMISS_LOCKSCREEN, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }
    }

    private val oemSmartspacerRepository by inject<OemSmartspacerRepository>()
    private val smartspaceRepository by inject<SmartspaceRepository>()
    private var hasRequestedDismiss: Boolean = false
    private var shouldLaunch = true

    private val keyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        super.onCreate(savedInstanceState)
        if(!verifySecurity()){
            shouldLaunch = false
            finish()
            return
        }
        val shouldDismiss = intent.getBooleanExtra(EXTRA_DISMISS_LOCKSCREEN, true)
        if(keyguardManager.isKeyguardLocked && shouldDismiss){
            dismissThenLaunch()
        }else{
            launch()
        }
    }

    private fun verifySecurity(): Boolean {
        return if(intent.extras?.containsKey(EXTRA_TOKEN) == true){
            val token = intent.getStringExtra(EXTRA_TOKEN)
            //Don't crash for an invalid token as it may simply need updating
            token == oemSmartspacerRepository.token
        }else{
            intent.verifySecurity()
            true
        }
    }

    override fun onDestroy() {
        //Handle edge case where dismiss listener doesn't return
        launchLocked()
        super.onDestroy()
    }

    private fun dismissThenLaunch() = lifecycleScope.launch {
        if(hasRequestedDismiss) return@launch
        hasRequestedDismiss = true
        //Allow time for the activity to start, seems to be a system issue? whenResumed breaks it
        delay(250L)
        keyguardManager.requestDismissKeyguard(
            this@TrampolineActivity,
            object: KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    launchLocked()
                }

                override fun onDismissCancelled() {
                    shouldLaunch = false
                    finish()
                }

                override fun onDismissError() {
                    shouldLaunch = false
                    finish()
                }
            }
        )
    }

    private fun launch() = whenCreated {
        launchLocked()
    }

    @Synchronized
    private fun launchLocked() {
        if(!shouldLaunch) return
        shouldLaunch = false
        if(intent.getBooleanExtra(EXTRA_LAUNCH_CALENDAR, false)){
            try {
                startActivity(getCalendarIntent())
            }catch (e: ActivityNotFoundException){
                //No calendar app installed
            }
        }
        intent.getStringExtra(EXTRA_TARGET_ID)?.let {
            val actionId = intent.getStringExtra(EXTRA_ACTION_ID)
            if(launchTarget(it, actionId)){
                finish()
                return
            }
        }
        intent.getStringExtra(EXTRA_ACTION_ID)?.let {
            if(launchAction(it)){
                finish()
                return
            }
        }
        intent.getStringExtra(EXTRA_URI_INTENT)?.let {
            startActivity(Intent.parseUri(it, 0))
        }
        intent.getParcelableExtraCompat(EXTRA_INTENT, Intent::class.java)?.let {
            startActivity(it)
        }
        intent.getParcelableExtraCompat(EXTRA_PENDING_INTENT, PendingIntent::class.java)
            ?.sendSafely()
        finish()
    }

    private fun launchTarget(targetId: String, actionId: String?): Boolean {
        val target = oemSmartspacerRepository.getSmartspaceTarget(targetId) ?: return false
        target.launch()
        smartspaceRepository.notifyClickEvent(targetId, actionId)
        return true
    }

    private fun launchAction(actionId: String): Boolean {
        val action = oemSmartspacerRepository.getSmartspaceAction(actionId) ?: return false
        action.launch()
        return true
    }

    private fun getCalendarIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.appendId(
                CalendarContract.CONTENT_URI.buildUpon().appendPath("time"),
                System.currentTimeMillis()
            ).build()
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    private fun SmartspaceTarget.launch() {
        val tapAction = templateData?.primaryItem?.tapAction
            ?: templateData?.subtitleItem?.tapAction
        val intent = tapAction?.intent ?: headerAction?.intent
        val pendingIntent = tapAction?.pendingIntent ?: headerAction?.pendingIntent
        when {
            intent != null -> startActivity(intent)
            pendingIntent != null -> pendingIntent.sendSafely()
        }
    }

    private fun SmartspaceAction.launch() {
        val subTapAction = subItemInfo?.tapAction
        val intent = subTapAction?.intent ?: intent
        val pendingIntent = subTapAction?.pendingIntent ?: pendingIntent
        when {
            intent != null -> startActivity(intent)
            pendingIntent != null -> pendingIntent.sendSafely()
        }
    }

}