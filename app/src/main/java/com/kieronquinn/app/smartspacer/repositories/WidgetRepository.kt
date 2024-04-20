package com.kieronquinn.app.smartspacer.repositories

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import androidx.annotation.RequiresApi
import com.kieronquinn.app.smartspacer.model.smartspace.Widget
import com.kieronquinn.app.smartspacer.sdk.utils.RemoteAdapter
import com.kieronquinn.app.smartspacer.sdk.utils.getClickPendingIntent
import com.kieronquinn.app.smartspacer.ui.views.appwidget.HeadlessAppWidgetHostView
import com.kieronquinn.app.smartspacer.ui.views.appwidget.HeadlessAppWidgetHostView.HeadlessWidgetEvent
import com.kieronquinn.app.smartspacer.utils.extensions.getDisplayPortraitHeight
import com.kieronquinn.app.smartspacer.utils.extensions.getDisplayPortraitWidth
import com.kieronquinn.app.smartspacer.utils.extensions.replaceUriActionsWithProxy
import com.kieronquinn.app.smartspacer.utils.extensions.updateAppWidgetSize
import com.kieronquinn.app.smartspacer.utils.remoteviews.RemoteCollectionItemsWrapper
import com.kieronquinn.app.smartspacer.utils.remoteviews.WidgetContextWrapper
import com.kieronquinn.app.smartspacer.widget.HeadlessAppWidgetHost
import com.kieronquinn.app.smartspacer.widget.HeadlessAppWidgetHost.OnProvidersChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import kotlin.math.roundToInt

interface WidgetRepository {

    val widgets: StateFlow<List<Widget>?>
    val providers: StateFlow<List<AppWidgetProviderInfo>>

    fun getProviders(): List<AppWidgetProviderInfo>

    fun getWidgetInfo(authority: String, id: String): AppWidgetProviderInfo?

    fun allocateAppWidgetId(): Int
    fun deallocateAppWidgetId(id: Int)
    fun bindAppWidgetIdIfAllowed(id: Int, provider: ComponentName): Boolean
    fun createConfigIntentSender(appWidgetId: Int): IntentSender
    fun clickAppWidgetIdView(appWidgetId: Int, identifier: String?, id: Int?)
    @RequiresApi(Build.VERSION_CODES.S)
    fun loadRemoteCollectionItems(
        smartspacerId: String, appWidgetId: Int, identifier: String?, id: Int?
    )
    fun loadAdapter(smartspacerId: String, appWidgetId: Int, identifier: String?, id: Int?)
    fun launchFillInIntent(pendingIntent: PendingIntent, fillInIntent: Intent)

}

class WidgetRepositoryImpl(
    private val context: Context,
    databaseRepository: DatabaseRepository,
    private val scope: CoroutineScope = MainScope()
): WidgetRepository, OnProvidersChangedListener {

    companion object {
        /**
         *  Naughty list of apps which cause crashes due to uncaught permission-protected provider
         *  calls. These widgets will never work with Smartspacer unless the OEMs come to their
         *  senses and stop blocking all apps other than their own from using their widgets.
         *
         *  <package name> : <class name> (leave class name empty for wildcard)
         *
         *  If you're a third party launcher developer looking for how this was "fixed" in
         *  Smartspacer, send me a message for access to a list shared between launcher developers
         *  for widgets to block, which you can also contribute to.
         */
        private val WIDGET_DENYLIST = mapOf(
            "com.hihonor.calendar" to "", //Honor Calendar (all blocked)
            "com.huawei.calendar" to "", //Huawei Calendar (all blocked)
            "com.hihonor.android.totemweather" to "", //Honor Weather (all blocked)
            "com.huawei.android.totemweather" to "", //Huawei Weather (all blocked)
            "com.vivo.widget.cleanspeed" to "", //Vivo "Clean Speed" (all blocked)
        )
    }

    private val contentResolver = context.contentResolver
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val databaseWidgets = databaseRepository.getWidgets()
    private val widgetJobs = mutableListOf<Job>()
    private val providersChanged = MutableSharedFlow<Unit>()
    private val widgetContext = WidgetContextWrapper(context)
    private val displayPortraitWidth = context.getDisplayPortraitWidth()
    private val displayPortraitHeight = context.getDisplayPortraitHeight()

    @VisibleForTesting
    val appWidgetHostPool = mutableListOf<HeadlessAppWidgetHostView>()

    @VisibleForTesting
    var appWidgetHost = HeadlessAppWidgetHost(
        widgetContext, 0, this
    ).also {
        it.startListening()
    }

    override val providers = providersChanged.mapLatest {
        appWidgetManager.installedProviders.filterIncompatible()
    }.stateIn(scope, SharingStarted.Eagerly, appWidgetManager.installedProviders.filterIncompatible())

    override val widgets = combine(
        databaseWidgets,
        providers
    ){ database, providers ->
        database.mapNotNull { widget ->
            val info = providers.getInfoForComponent(widget.component)
                ?: return@mapNotNull null
            @Suppress("CloseWidget")
            Widget(
                context,
                widget.appWidgetId,
                info,
                widget.authority,
                widget.id,
                widget.type,
                widget.packageName
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun getProviders(): List<AppWidgetProviderInfo> {
        return providers.value
    }

    private fun setupDatabaseRefresh() {
        scope.launch {
            var widgetsCache = widgets.value
            widgets.collect {
                widgetsCache?.forEach { target -> target.close() }
                widgetsCache = it
            }
        }
    }

    private fun setupWidgets() = scope.launch {
        widgets.filterNotNull().debounce(250L).collect {
            handleWidgets(it)
        }
    }

    private fun handleWidgets(widgets: List<Widget>) {
        widgetJobs.forEach {
            it.cancel()
        }
        widgetJobs.clear()
        appWidgetHostPool.forEach {
            appWidgetHost.destroyView(it)
        }
        synchronized(appWidgetHostPool) {
            appWidgetHostPool.clear()
        }
        widgets.forEach { widget ->
            scope.launch(Dispatchers.IO) {
                val view = appWidgetHost.createView(
                    context, widget.appWidgetId, widget.info
                ) as HeadlessAppWidgetHostView
                synchronized(appWidgetHostPool) {
                    appWidgetHostPool.add(view)
                }
                view.setup(widget)
            }.also {
                widgetJobs.add(it)
            }
        }
    }

    private suspend fun HeadlessAppWidgetHostView.setup(widget: Widget) {
        info = widget.info
        appWidgetId = widget.appWidgetId
        widget.getPluginConfig().filterNotNull().flatMapLatest {
            val options = appWidgetManager.getAppWidgetOptions(widget.appWidgetId)
            val width = it.width ?: displayPortraitWidth
            val height = it.height ?: (displayPortraitHeight / 4f).roundToInt()
            updateAppWidgetSize(context, width.toFloat(), height.toFloat(), options)
            this@setup
        }.collectLatest {
            when(it) {
                is HeadlessWidgetEvent.RemoteViews -> {
                    val remoteViews = it.remoteViews?.remoteViews
                        ?.replaceUriActionsWithProxy(context, widget.sourcePackage)
                    widget.onWidgetChanged(remoteViews)
                }
                is HeadlessWidgetEvent.Adapter -> {
                    widget.onViewDataChanged(it.viewId)
                }
            }
        }
    }

    private fun List<AppWidgetProviderInfo>.getInfoForComponent(name: String): AppWidgetProviderInfo? {
        val component = ComponentName.unflattenFromString(name) ?: return null
        return firstOrNull { it.provider == component }
    }

    init {
        setupDatabaseRefresh()
        setupWidgets()
    }

    override fun getWidgetInfo(authority: String, id: String): AppWidgetProviderInfo? {
        return Widget.getAppWidgetProviderInfo(contentResolver, authority, id)
    }

    override fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    override fun deallocateAppWidgetId(id: Int) {
        appWidgetHost.deleteAppWidgetId(id)
    }

    override fun bindAppWidgetIdIfAllowed(id: Int, provider: ComponentName): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(id, provider)
    }

    override fun createConfigIntentSender(appWidgetId: Int): IntentSender {
        return appWidgetHost.getIntentSenderForConfigureActivity(appWidgetId, 0)
    }

    override fun clickAppWidgetIdView(appWidgetId: Int, identifier: String?, id: Int?) {
        val hostView = appWidgetHostPool.firstOrNull {
            it.appWidgetId == appWidgetId
        } ?: return
        when {
            identifier != null -> {
                hostView.clickView(identifier)
            }
            id != null -> {
                hostView.clickView(id)
            }
        }
   }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun loadRemoteCollectionItems(
        smartspacerId: String,
        appWidgetId: Int,
        identifier: String?,
        id: Int?
    ) {
        scope.launch(Dispatchers.IO) {
            val remoteAdapter = getRemoteCollectionAdapter(appWidgetId, identifier, id)
                ?: return@launch
            widgets.value?.firstOrNull {
                it.id == smartspacerId && it.appWidgetId == appWidgetId
            }?.run {
                onAdapterLoaded(remoteAdapter)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getRemoteCollectionAdapter(
        appWidgetId: Int,
        identifier: String?,
        id: Int?
    ): RemoteAdapter? {
        val hostView = synchronized(appWidgetHostPool) {
            appWidgetHostPool.firstOrNull {
                if(it == null) return@firstOrNull false
                it.appWidgetId == appWidgetId
            } ?: return null
        }
        val adapterView = hostView.findAdapterView(identifier, id) ?: return null
        val adapter = hostView.findRemoteViewsCollectionListAdapter(identifier, id)
            ?: return null
        return RemoteAdapter(
            RemoteCollectionItemsWrapper(context, adapter),
            adapterView.getClickPendingIntent(),
            identifier,
            id
        )
    }

    override fun onProvidersChanged() {
        scope.launch {
            providersChanged.emit(Unit)
        }
    }

    override fun loadAdapter(
        smartspacerId: String,
        appWidgetId: Int,
        identifier: String?,
        id: Int?
    ) {
        scope.launch(Dispatchers.IO) {
            val remoteAdapter = getRemoteAdapter(appWidgetId, identifier, id)
                ?: return@launch
            widgets.value?.firstOrNull {
                it.id == smartspacerId && it.appWidgetId == appWidgetId
            }?.run {
                onAdapterLoaded(remoteAdapter)
            }
        }
    }

    private suspend fun getRemoteAdapter(
        appWidgetId: Int,
        identifier: String?,
        id: Int?
    ): RemoteAdapter? {
        val hostView = synchronized(appWidgetHostPool) {
            appWidgetHostPool.firstOrNull {
                if (it == null) return@firstOrNull false
                it.appWidgetId == appWidgetId
            } ?: return null
        }
        val adapterView = hostView.findAdapterView(identifier, id) ?: return null
        val adapter = hostView.findRemoteViewsAdapter(identifier, id) ?: return null
        return RemoteAdapter(
            adapter.awaitConnected(),
            adapterView.getClickPendingIntent(),
            identifier,
            id
        )
    }

    override fun launchFillInIntent(pendingIntent: PendingIntent, fillInIntent: Intent) {
        context.startIntentSender(
            pendingIntent.intentSender, fillInIntent, 0, 0, 0
        )
    }

    private fun List<AppWidgetProviderInfo>.filterIncompatible(): List<AppWidgetProviderInfo> {
        return filterNot { provider ->
            val packageName = provider.provider.packageName
            val className = provider.provider.className
            WIDGET_DENYLIST.any {
                it.key == packageName && (it.value.isEmpty() || it.value == className)
            }
        }
    }

}