package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.getSerializableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WidgetPageChangeReceiver: BroadcastReceiver(), KoinComponent {

    private val appWidgetRepository by inject<AppWidgetRepository>()

    companion object {
        private const val EXTRA_DIRECTION = "direction"
        private const val EXTRA_APP_WIDGET_ID = "app_widget_id"

        fun getIntent(context: Context, appWidgetId: Int, direction: Direction): Intent {
            return Intent(context, WidgetPageChangeReceiver::class.java).apply {
                applySecurity(context)
                putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
                putExtra(EXTRA_DIRECTION, direction)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.verifySecurity()
        val direction = intent.getSerializableExtraCompat(EXTRA_DIRECTION, Direction::class.java)
            ?: return
        val appWidgetId = intent.getIntExtra(EXTRA_APP_WIDGET_ID, -1).takeIf { it > 0 }
            ?: return
        when(direction) {
            Direction.NEXT -> appWidgetRepository.nextPage(appWidgetId)
            Direction.PREVIOUS -> appWidgetRepository.previousPage(appWidgetId)
        }
    }

    enum class Direction {
        NEXT, PREVIOUS
    }

}