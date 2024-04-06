package com.kieronquinn.app.smartspacer.service

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.ListWidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.ui.activities.WidgetOptionsMenuActivity
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartspacerListWidgetRemoteViewsService: RemoteViewsService() {

    companion object {
        private const val EXTRA_APP_WIDGET_ID = "app_widget_id"
        private const val EXTRA_OWNER = "owner"

        fun createIntent(context: Context, appWidgetId: Int, owner: String): Intent {
            return Intent(context, SmartspacerListWidgetRemoteViewsService::class.java).apply {
                putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
                putExtra(EXTRA_OWNER, owner)
            }
        }
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(EXTRA_APP_WIDGET_ID, -1)
        val owner = intent.getStringExtra(EXTRA_OWNER)!!
        return SmartspacerListWidgetRemoteViewsFactory(this, appWidgetId, owner)
    }

}

private class SmartspacerListWidgetRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
    private val owner: String
): RemoteViewsService.RemoteViewsFactory, KoinComponent {

    private val appWidgetRepository by inject<AppWidgetRepository>()

    private var sessionState: ListWidgetSmartspacerSessionState? = null

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    override fun onDestroy() {
        sessionState = null
    }

    override fun getCount(): Int {
        return sessionState?.pages?.size ?: 0
    }

    override fun getViewAt(position: Int): RemoteViews {
        val session = sessionState
        val page = session?.pages?.getOrNull(position) ?: return loadingView
        val overflowIntent = WidgetOptionsMenuActivity.getIntent(
            context, page.holder.page, appWidgetId, owner
        )
        return page.view.load(session.widgetConfig, overflowIntent)
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_smartspacer_loading_list)
    }

    override fun getViewTypeCount(): Int {
        return SmartspaceView.ViewType.entries.size
    }

    override fun getItemId(position: Int): Long {
        return sessionState?.pages?.getOrNull(position)
            ?.holder?.page?.smartspaceTargetId?.hashCode()?.toLong() ?: -1L
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun loadData() {
        sessionState = appWidgetRepository.getListSessionState(appWidgetId)
    }

    private fun SmartspaceView.load(widget: AppWidget, overflowIntent: Intent): RemoteViews {
        return with(appWidgetRepository) {
            context.getPageRemoteViews(
                appWidgetId,
                this@load,
                widget,
                true,
                overflowIntent
            )
        }
    }

}