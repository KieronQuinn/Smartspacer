package android.widget

import android.appwidget.AppWidgetHostHidden
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.IntentSender
import android.widget.SmartspacerAppWidgetHost.ProxyAppWidgetHostListener
import com.kieronquinn.app.smartspacer.utils.extensions.getIntentSenderForConfigureActivityCompat

/**
 *  AppWidgetHost which supports multiple of the same App Widget ID, by intercepting requests to
 *  [setListener] and using [ProxyAppWidgetHostListener] to fan out calls to multiple hosts.
 *
 *  Since this does not rely on copying the entire class, it should hopefully be safe-ish.
 */
abstract class SmartspacerAppWidgetHost(context: Context, id: Int): AppWidgetHostHidden(context, id) {

    private val proxyListeners = HashMap<Int, ProxyAppWidgetHostListener>()

    override fun setListener(appWidgetId: Int, listener: AppWidgetHostListener) {
        val proxy = proxyListeners[appWidgetId] ?: ProxyAppWidgetHostListener().also {
            proxyListeners[appWidgetId] = it
        }
        proxy.addProxyListener(listener)
        super.setListener(appWidgetId, proxy)
    }


    fun destroyView(view: AppWidgetHostView) {
        val appWidgetId = view.appWidgetId
        proxyListeners[appWidgetId]?.removeProxyListener(view as AppWidgetHostListener)
    }

    fun getIntentSenderForConfigureActivity(appWidgetId: Int, intentFlags: Int): IntentSender {
        return getIntentSenderForConfigureActivityCompat(appWidgetId, intentFlags)
    }

    private class ProxyAppWidgetHostListener: AppWidgetHostListener {

        private val proxyListeners = HashSet<AppWidgetHostListener>()

        override fun onUpdateProviderInfo(appWidget: AppWidgetProviderInfo?) {
            runWithProxies {
                onUpdateProviderInfo(appWidget)
            }
        }

        override fun updateAppWidget(views: RemoteViews?) {
            runWithProxies {
                updateAppWidget(views)
            }
        }

        override fun onViewDataChanged(viewId: Int) {
            runWithProxies {
                onViewDataChanged(viewId)
            }
        }

        fun addProxyListener(proxy: AppWidgetHostListener){
            synchronized(proxyListeners) {
                proxyListeners.add(proxy)
            }
        }

        fun removeProxyListener(proxy: AppWidgetHostListener) {
            synchronized(proxyListeners) {
                proxyListeners.remove(proxy)
            }
        }

        private fun <T> runWithProxies(block: AppWidgetHostListener.() -> T) {
            synchronized(proxyListeners) {
                proxyListeners.forEach {
                    block(it)
                }
            }
        }

    }

}