package com.kieronquinn.app.smartspacer.ui.activities

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews.RemoteResponse
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport.Companion.PACKAGE_PIXEL_LAUNCHER
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.sdk.model.RemoteOnClickResponse
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.sdk.utils.sendSafely
import com.kieronquinn.app.smartspacer.utils.extensions.EXPORTED_WEATHER_COMPONENT
import com.kieronquinn.app.smartspacer.utils.extensions.SMARTSPACE_EXPORTED_COMPONENT
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.toRemoteResponse
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import org.koin.android.ext.android.inject
import java.util.UUID

class ExportedSmartspaceTrampolineProxyActivity: AppCompatActivity() {

    companion object {
        private const val SMARTSPACE_INTENT_EXTRA =
            "com.google.android.apps.gsa.smartspace.extra.SMARTSPACE_INTENT"

        private const val SMARTSPACER_REMINDER_ID_EXTRA =
            "com.google.android.apps.search.assistant.platform.pcp.proto.reminder.eventdata.extra.REMINDER_ID"

        private const val SMARTSPACER_INTENT_EXTRA_ACTION_GM =
            "com.google.android.gm.intent.VIEW_PLID"

        private const val URL_GOOGLE_ASSISTANT_REMINDERS =
            "https://assistant.google.com/reminders/id"

        private val WEATHER_COMPONENT = ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.search.weather.WeatherActivity"
        )

        private const val ACTION_WEATHER = "com.google.android.apps.weather.action.VIEW_LOCATION_EXTERNAL"
        private const val EXTRA_REMOTE_RESPONSE = "remote_response"

        fun getPendingIntent(context: Context, extra: RemoteResponse): PendingIntent {
            return PendingIntent.getActivity(
                context,
                UUID.randomUUID().hashCode(),
                getIntent(context, extra),
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun getIntent(context: Context, extra: RemoteResponse): Intent {
            return Intent(context, ExportedSmartspaceTrampolineProxyActivity::class.java).apply {
                putExtra(EXTRA_REMOTE_RESPONSE, extra.toRemoteResponse().toBundle())
                applySecurity(context)
            }
        }
    }

    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.verifySecurity()
        when {
            intent.hasExtra(SMARTSPACE_INTENT_EXTRA) -> if(startProxyIntent()) {
                finish()
            }
            intent.hasExtra(EXTRA_REMOTE_RESPONSE) -> startRemoteResponseIntent()
            else -> finish()
        }
    }

    /**
     *  Extracts Smartspacer trampoline intents and attempts to open them via exported activities.
     *
     *  Returns whether to finish immediately or whether finishing should be handled by further
     *  calls (eg. if a coroutine is called).
     */
    private fun startProxyIntent(): Boolean {
        val rootIntent = intent
        val intent = rootIntent.getStringExtra(SMARTSPACE_INTENT_EXTRA)?.let {
            Intent.parseUri(it, 0)
        }
        val launchIntent = when {
            intent?.component == WEATHER_COMPONENT -> createLaunchIntent {
                action = intent.action
                `package` = intent.`package`
                flags = intent.flags
                extras?.clear()
                intent.extras?.let {
                    putExtras(it)
                }
                component = EXPORTED_WEATHER_COMPONENT
            }
            intent?.action == ACTION_WEATHER -> intent
            intent?.action == SMARTSPACER_INTENT_EXTRA_ACTION_GM -> {
                startGmailPlid(intent, rootIntent)
                return false
            }
            rootIntent?.getStringExtra(SMARTSPACER_REMINDER_ID_EXTRA) != null -> createLaunchIntent {
                action = Intent.ACTION_VIEW
                data = rootIntent.createAssistantReminderUri() ?: return@createLaunchIntent
            }
            intent?.dataString != null -> createLaunchIntent {
                action = Intent.ACTION_VIEW
                data = intent.data
            }
            //Unknown type
            else -> {
                Log.d("Smartspacer", "Couldn't handle proxy intent ${intent?.toUri(0)}")
                return true
            }
        }
        try {
            startActivity(launchIntent)
        }catch (e: Exception) {
            Log.d("Smartspacer", "Failed to launch intent ${launchIntent.toUri(0)}", e)
            //Invalid Intent?
        }
        return true
    }

    private fun startGmailPlid(intent: Intent, rootIntent: Intent) = whenCreated {
        if(intent.`package` != "com.google.android.gm") return@whenCreated
        val plid = intent.getStringExtra("plid") ?: return@whenCreated
        val hasLaunched = shizukuServiceRepository.runWithSuiService {
            if(it.isRoot) {
                //The Gmail app can only open plid links from root, due to security restrictions
                rootIntent.component = SMARTSPACE_EXPORTED_COMPONENT
                it.startActivityPrivileged(rootIntent)
                true
            }else false
        }.unwrap()
        if(hasLaunched == true){
            finish()
            return@whenCreated
        }
        //No root means we cannot use the app and will have to open in the browser instead
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://mail.google.com/mail/?extsrc=sync&client=g&plid=$plid")
        }
        startActivity(fallbackIntent)
        finish()
    }

    private fun Intent.createAssistantReminderUri(): Uri? {
        return Uri.parse(URL_GOOGLE_ASSISTANT_REMINDERS).buildUpon().appendPath(
            getStringExtra(SMARTSPACER_REMINDER_ID_EXTRA) ?: return null
        ).build()
    }

    /**
     *  Checks if the RemoteResponse pending intent will open the Pixel Launcher, ignores it if so,
     *  otherwise launches it
     */
    private fun startRemoteResponseIntent() = whenCreated {
        val remoteResponse = intent
            .getParcelableExtraCompat(EXTRA_REMOTE_RESPONSE, Bundle::class.java)?.let {
                RemoteOnClickResponse.RemoteResponse(it)
            }
        val intent = remoteResponse?.pendingIntent?.getIntent()
        if (intent != null) {
            // Only launch the intent if it's not going to open the Pixel Launcher
            if (intent.component?.packageName != PACKAGE_PIXEL_LAUNCHER) {
                remoteResponse.pendingIntent?.sendSafely()
            }
        } else {
            // Failed to resolve the intent, try launching anyway
            remoteResponse?.pendingIntent?.sendSafely()
        }
        finish()
    }

    private suspend fun PendingIntent.getIntent() = shizukuServiceRepository.runWithService {
        it.resolvePendingIntent(this)
    }.unwrap()

    private fun <T> createLaunchIntent(block: Intent.() -> T?): Intent {
        return Intent().apply {
            block(this)
            action = intent.action
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

}