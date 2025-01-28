package com.kieronquinn.app.smartspacer.ui.views.appwidget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.AdapterView
import android.widget.RemoteViews
import android.widget.RemoteViews.RemoteCollectionItems
import android.widget.RemoteViewsAdapter.EXTRA_REMOTEADAPTER_APPWIDGET_ID
import androidx.annotation.RequiresApi
import com.kieronquinn.app.smartspacer.sdk.utils.findViewByIdentifier
import com.kieronquinn.app.smartspacer.ui.views.RoundedCornersEnforcingAppWidgetHostView
import com.kieronquinn.app.smartspacer.ui.views.appwidget.HeadlessAppWidgetHostView.HeadlessWidgetEvent
import com.kieronquinn.app.smartspacer.utils.extensions.createPackageContextOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.extractAdapterIntent
import com.kieronquinn.app.smartspacer.utils.extensions.extractRemoteCollectionIntent
import com.kieronquinn.app.smartspacer.utils.extensions.extractRemoteCollectionItems
import com.kieronquinn.app.smartspacer.utils.extensions.getActionsIncludingNested
import com.kieronquinn.app.smartspacer.utils.extensions.getCollectionCache
import com.kieronquinn.app.smartspacer.utils.extensions.isAtLeastBaklava
import com.kieronquinn.app.smartspacer.utils.extensions.isRemoteCollectionItemListAdapter
import com.kieronquinn.app.smartspacer.utils.extensions.isRemoteViewsAdapterIntent
import com.kieronquinn.app.smartspacer.utils.remoteviews.RemoteViewsFactoryWrapper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class HeadlessAppWidgetHostView(context: Context): RoundedCornersEnforcingAppWidgetHostView(context), Flow<HeadlessWidgetEvent> {

    private val callbacks = ArrayList<Callback?>()
    private var lastRemoteViews: RemoteViewsContainer? = null
    private val scope = MainScope()

    private val appWidgetUpdate = MutableStateFlow<RemoteViews?>(null)
    private val adapterPool = HashMap<Int, RemoteViewsFactoryWrapper>()

    var appWidgetId: Int? = null
    var info: AppWidgetProviderInfo? = null

    override fun setAppWidget(appWidgetId: Int, info: AppWidgetProviderInfo?) {
        this.appWidgetId = appWidgetId
        this.info = info
        adapterPool.values.forEach {
            it.disconnect()
        }
        adapterPool.clear()
        super.setAppWidget(appWidgetId, info)
    }

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        scope.launch {
            appWidgetUpdate.emit(remoteViews)
        }
    }

    private fun debouncedUpdateAppWidget(remoteViews: RemoteViews?) {
        try {
            super.updateAppWidget(remoteViews)
        }catch (e: ArrayIndexOutOfBoundsException){
            return //Widget has gone
        }
        lastRemoteViews = remoteViews?.let {
            RemoteViewsContainer(it)
        }
        callbacks.forEach {
            if(it == null) return@forEach
            it.onRemoteViewsChanged(lastRemoteViews)
        }
    }

    private fun setupUpdateAppWidgetDebounced() = scope.launch {
        appWidgetUpdate.debounce(500L).collect {
            debouncedUpdateAppWidget(it)
        }
    }

    init {
        setupUpdateAppWidgetDebounced()
    }

    override fun onViewDataChanged(viewId: Int) {
        super.onViewDataChanged(viewId)
        callbacks.forEach {
            it?.onViewDataChanged(viewId)
        }
    }

    override suspend fun collect(collector: FlowCollector<HeadlessWidgetEvent>) {
        asCallbackFlow().collect(collector)
    }

    private fun asCallbackFlow() = callbackFlow {
        val callback = object: Callback {
            override fun onRemoteViewsChanged(remoteViews: RemoteViewsContainer?) {
                trySend(HeadlessWidgetEvent.RemoteViews(remoteViews))
            }

            override fun onViewDataChanged(viewId: Int) {
                trySend(HeadlessWidgetEvent.Adapter(viewId))
            }
        }
        callbacks.add(callback)
        trySend(HeadlessWidgetEvent.RemoteViews(lastRemoteViews))
        awaitClose {
            callbacks.remove(callback)
        }
    }

    private fun findViewByIdentifier(identifier: String): View? {
        val packageName = info?.provider?.packageName ?: return null
        val packageContext = context.createPackageContextOrNull(
            packageName, Context.CONTEXT_RESTRICTED
        ) ?: return null
        return findViewByIdentifier<View>(identifier, packageContext)
    }

    fun clickView(identifier: String) {
        findViewByIdentifier(identifier)?.performClick()
    }

    fun clickView(id: Int) {
        findViewById<View>(id)?.performClick()
    }

    fun findRemoteViewsAdapter(identifier: String?, id: Int?): RemoteViewsAdapter? {
        val viewId = identifier?.let { findViewByIdentifier(it)?.id } ?: id ?: return null
        return adapterPool[viewId]?.let {
            RemoteViewsAdapter.RemoteWrapper(it)
        } ?: run {
            val remoteViews = lastRemoteViews?.remoteViews
            val packageName = info?.provider?.packageName ?: return null
            val intent = remoteViews?.findRemoteViewsAdapters()?.firstOrNull {
                it.first == viewId
            }?.second
            if(intent != null) {
                return wrapRemoteViewsAdapterIntent(packageName, viewId, intent)?.let {
                    RemoteViewsAdapter.RemoteWrapper(it)
                }
            }
            if(isAtLeastBaklava()) {
                val collectionIntent = remoteViews?.findRemoteViewsCollectionListIntents()
                    ?.firstOrNull { it.first == viewId }?.second?.toUri(0)
                val items = remoteViews?.getCollectionCache()?.get(collectionIntent)
                if(items != null) {
                    remoteViews.getCollectionCache().let {
                        return RemoteViewsAdapter.CollectionItems(items)
                    }
                }
            }
            return null
        }
    }

    sealed class RemoteViewsAdapter {
        data class RemoteWrapper(val wrapper: RemoteViewsFactoryWrapper): RemoteViewsAdapter()
        data class CollectionItems(val items: RemoteCollectionItems): RemoteViewsAdapter()
    }

    private fun wrapRemoteViewsAdapterIntent(
        packageName: String,
        viewId: Int,
        intent: Intent
    ): RemoteViewsFactoryWrapper? {
        intent.apply {
            putExtra(EXTRA_REMOTEADAPTER_APPWIDGET_ID, appWidgetId)
        }
        val remoteContext = context.createPackageContextOrNull(packageName) ?: return null
        return RemoteViewsFactoryWrapper(remoteContext, intent, viewId) {
            adapterPool.remove(viewId)
        }.also {
            adapterPool[viewId] = it
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun findRemoteViewsCollectionListAdapter(identifier: String?, id: Int?): RemoteCollectionItems? {
        val viewId = identifier?.let { findViewByIdentifier(it)?.id } ?: id ?: return null
        return lastRemoteViews?.remoteViews?.findRemoteViewsCollectionListAdapters()?.firstOrNull {
            it.first == viewId
        }?.second
    }

    fun findAdapterView(identifier: String?, id: Int?): AdapterView<*>? {
        val viewId = identifier?.let { findViewByIdentifier(it)?.id } ?: id ?: return null
        return try {
            findViewById(viewId)
        }catch (e: Exception){
            null
        }
    }

    private fun RemoteViews.findRemoteViewsAdapters(): List<Pair<Int, Intent>> {
        return getActionsIncludingNested().filter { it.isRemoteViewsAdapterIntent() }.map {
            it.extractAdapterIntent()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun RemoteViews.findRemoteViewsCollectionListAdapters(): List<Pair<Int, RemoteCollectionItems>> {
        return getActionsIncludingNested().filter {
            it.isRemoteCollectionItemListAdapter()
        }.mapNotNull {
            it.extractRemoteCollectionItems()
        }
    }

    //Requires 36
    @RequiresApi(Build.VERSION_CODES.S)
    private fun RemoteViews.findRemoteViewsCollectionListIntents(): List<Pair<Int, Intent>> {
        return getActionsIncludingNested().filter {
            it.isRemoteCollectionItemListAdapter()
        }.mapNotNull {
            it.extractRemoteCollectionIntent()
        }
    }

    private interface Callback {
        fun onRemoteViewsChanged(remoteViews: RemoteViewsContainer?)
        fun onViewDataChanged(viewId: Int)
    }

    sealed class HeadlessWidgetEvent {
        data class RemoteViews(val remoteViews: RemoteViewsContainer?): HeadlessWidgetEvent()
        data class Adapter(val viewId: Int): HeadlessWidgetEvent()
    }

    data class RemoteViewsContainer(
        val remoteViews: RemoteViews,
        val tag: Long = System.currentTimeMillis()
    ) {

        override fun equals(other: Any?): Boolean {
            //RemoteViews does not have an equals so we check the creation times
            return (other as? RemoteViewsContainer)?.tag == tag
        }

        override fun hashCode(): Int {
            var result = remoteViews.hashCode()
            result = 31 * result + tag.hashCode()
            return result
        }

    }

}