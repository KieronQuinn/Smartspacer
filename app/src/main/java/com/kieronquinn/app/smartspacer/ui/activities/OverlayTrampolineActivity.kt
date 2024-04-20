package com.kieronquinn.app.smartspacer.ui.activities

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.window.SplashScreen
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.ActivityOptions_fromBundle
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity


/**
 *  Required to trampoline PendingIntents from the launcher overlay to allow third party apps to
 *  be launched from the background.
 */
class OverlayTrampolineActivity: AppCompatActivity() {

    companion object {
        private const val EXTRA_PENDING_INTENT = "pending_intent"
        private const val EXTRA_ACTIVITY_OPTIONS = "activity_options"
        private const val EXTRA_INTENT = "intent"

        fun trampoline(
            view: View,
            context: Context,
            pendingIntent: PendingIntent,
            options: ActivityOptions? = null,
            intent: Intent? = null
        ) {
            val trampolineIntent = createIntent(context, pendingIntent, options, intent).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                sourceBounds = intent?.sourceBounds
            }
            val activityOptions = ActivityOptions.makeScaleUpAnimation(
                view,
                0,
                0,
                view.width,
                view.height
            ).setSplashStyle().toBundle()
            context.startActivity(trampolineIntent, activityOptions)
        }

        private fun ActivityOptions.setSplashStyle() = apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON)
            }
        }

        private fun createIntent(
            context: Context,
            pendingIntent: PendingIntent,
            options: ActivityOptions?,
            intent: Intent?
        ): Intent {
            val smartspacerContext = context.createPackageContext(
                BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY
            )
            return Intent(smartspacerContext, OverlayTrampolineActivity::class.java).apply {
                applySecurity(smartspacerContext)
                putExtra(EXTRA_PENDING_INTENT, pendingIntent)
                putExtra(EXTRA_ACTIVITY_OPTIONS, options?.toBundle())
                putExtra(EXTRA_INTENT, intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        intent.verifySecurity()
        val pendingIntent =
            intent.getParcelableExtraCompat(EXTRA_PENDING_INTENT, PendingIntent::class.java)
        val activityOptions =
            intent.getParcelableExtraCompat(EXTRA_ACTIVITY_OPTIONS, Bundle::class.java)?.let {
                ActivityOptions_fromBundle(it)
            }
        val intent = intent.getParcelableExtraCompat(EXTRA_INTENT, Intent::class.java)
        if(pendingIntent != null) {
            startPendingIntent(pendingIntent, Pair(intent, activityOptions))
        }
        finishAndRemoveTask()
    }

    private fun startPendingIntent(
        pendingIntent: PendingIntent,
        options: Pair<Intent?, ActivityOptions?>
    ): Boolean {
        val activityOptions = options.second ?: ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 34) {
            activityOptions.pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            activityOptions.pendingIntentCreatorBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        try {
            // The NEW_TASK flags are applied through the activity options and not as a part of
            // the call to startIntentSender() to ensure that they are consistently applied to
            // both mutable and immutable PendingIntents.
            startIntentSender(
                pendingIntent.intentSender,
                options.first,
                0,
                0,
                0,
                activityOptions.toBundle()
            )
        } catch (e: SendIntentException) {
            Log.e("RemoteViews", "Cannot send pending intent: ", e)
            return false
        } catch (e: Exception) {
            Log.e("RemoteViews", "Cannot send pending intent due to unknown exception: ", e)
            return false
        }
        return true
    }

}