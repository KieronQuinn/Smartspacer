package com.kieronquinn.app.smartspacer.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.IntentSender
import android.widget.SmartspacerAppWidgetHost
import android.widget.SmartspacerAppWidgetHostCompat
import com.kieronquinn.app.smartspacer.ui.views.appwidget.ExpandedAppWidgetHostView

class ExpandedAppWidgetHost {

    companion object {

        @SuppressLint("PrivateApi")
        private fun supportsSystemHost(): Boolean {
            return try {
                Class.forName("android.appwidget.AppWidgetHost\$AppWidgetHostListener")
                true
            }catch (e: ClassNotFoundException){
                false
            }
        }

        fun create(context: Context, hostId: Int): AppWidgetHost {
            return if(supportsSystemHost()) {
                ExpandedAppWidgetHostAPI33(context, hostId)
            }else{
                ExpandedAppWidgetHostAPI29(context, hostId)
            }
        }
    }

    fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return ExpandedAppWidgetHostView(context)
    }

    private class ExpandedAppWidgetHostAPI33(
        context: Context,
        id: Int
    ): SmartspacerAppWidgetHost(context, id), AppWidgetHost {

        private val impl = ExpandedAppWidgetHost()

        override fun createView(
            context: Context,
            appWidgetId: Int,
            id: String,
            appWidget: AppWidgetProviderInfo?
        ): AppWidgetHostView {
            return super.createView(context, appWidgetId, appWidget)
        }

        override fun destroyView(view: ExpandedAppWidgetHostView) {
            super.destroyView(view)
        }

        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?
        ): AppWidgetHostView {
            return impl.onCreateView(context, appWidgetId, appWidget)
        }

    }

    private class ExpandedAppWidgetHostAPI29(
        context: Context,
        id: Int
    ): SmartspacerAppWidgetHostCompat(context, id), AppWidgetHost {

        private val impl = ExpandedAppWidgetHost()

        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?
        ): AppWidgetHostView {
            return impl.onCreateView(context, appWidgetId, appWidget)
        }

    }

}

interface AppWidgetHost {

    fun createView(
        context: Context,
        appWidgetId: Int,
        id: String,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView

    fun destroyView(view: ExpandedAppWidgetHostView)
    fun allocateAppWidgetId(): Int
    fun deleteAppWidgetId(appWidgetId: Int)
    fun startListening()
    fun getIntentSenderForConfigureActivity(appWidgetId: Int, intentFlags: Int): IntentSender

}

