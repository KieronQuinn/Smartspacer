package com.kieronquinn.app.smartspacer.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.utils.extensions.EXPORTED_WEATHER_COMPONENT
import com.kieronquinn.app.smartspacer.utils.extensions.SMARTSPACE_EXPORTED_COMPONENT
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import org.koin.android.ext.android.inject

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
    }

    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.verifySecurity()
        if(startProxyIntent()) {
            finish()
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
            else -> return true
        }
        try {
            startActivity(launchIntent)
        }catch (e: Exception) {
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