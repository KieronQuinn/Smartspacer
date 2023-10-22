package com.kieronquinn.app.smartspacer.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.utils.extensions.EXPORTED_WEATHER_COMPONENT
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity

class ExportedSmartspaceTrampolineProxyActivity: AppCompatActivity() {

    companion object {
        private const val SMARTSPACE_INTENT_EXTRA =
            "com.google.android.apps.gsa.smartspace.extra.SMARTSPACE_INTENT"

        private val WEATHER_COMPONENT = ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.search.weather.WeatherActivity"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.verifySecurity()
        val proxyIntent = getProxyIntent()
        if(proxyIntent != null){
            try {
                startActivity(proxyIntent)
            }catch (e: Exception) {
                //Invalid Intent?
            }
        }
        finish()
    }

    private fun getProxyIntent(): Intent? {
        val intent = intent.getStringExtra(SMARTSPACE_INTENT_EXTRA)?.let {
            Intent.parseUri(it, 0)
        }
        return when {
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
            intent?.dataString != null -> createLaunchIntent {
                action = Intent.ACTION_VIEW
                data = intent.data
            }
            else -> null
        }
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