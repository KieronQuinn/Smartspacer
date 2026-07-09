package com.kieronquinn.app.smartspacer.components.smartspace

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
import android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
import android.content.pm.ParceledListSlice
import android.content.pm.ShortcutInfo
import android.os.DeadObjectException
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.components.smartspace.compat.TargetMerger.Companion.BLANK_TARGET_PREFIX
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcut
import com.kieronquinn.app.smartspacer.model.appshortcuts.ShortcutQueryWrapper
import com.kieronquinn.app.smartspacer.model.database.ExpandedAppWidget
import com.kieronquinn.app.smartspacer.model.doodle.DoodleImage
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedBackground
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.BaseShortcut
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession.Complications.Complication
import com.kieronquinn.app.smartspacer.utils.extensions.createFakeWidgetProviderInfo
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import com.kieronquinn.app.smartspacer.utils.extensions.split
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.inject
import java.util.LinkedList
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession.Complications as ExpandedComplications

@Suppress("CloseTarget")
class ExpandedSmartspacerSession(
    private val context: Context,
    override val sessionId: SmartspaceSessionId,
    private val collectInto: suspend (List<Item>) -> Unit
): BaseSmartspacerSession<Item, SmartspaceSessionId>(
    context, SmartspaceConfig(0, UiSurface.HOMESCREEN, ""), sessionId
) {

    companion object {
        private const val MAX_SHORTCUTS = 10
    }

    /** Raw page targets in session order, updated before any blank-target splitting. */
    val rawPageTargets = MutableStateFlow<List<SmartspaceTarget>>(emptyList())

    private val expandedRepository by inject<ExpandedRepository>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()
    private val databaseRepository by inject<DatabaseRepository>()
    private val widgetRepository by inject<WidgetRepository>()
    private val wallpaperRepository by inject<WallpaperRepository>()
    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()
    private val smartspaceRepository by inject<SmartspaceRepository>()
    private val topInset = MutableStateFlow(0)
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    override val targetCount = flowOf(Integer.MAX_VALUE)

    // Disable the 16ms debounce: collectLatest already cancels stale work, so the debounce
    // only adds delay on startup (up to 600ms while stateIn flows settle to real values).
    override val targetCollectionDebounce: Long = 0L

    // Pre-seeded so uiSurface doesn't block three separate combines (convert, loadSmartspaceHolders,
    // applyActionOverrides) while waiting for lockscreenShowing()'s first BroadcastReceiver event.
    override val uiSurface = context.lockscreenShowing().mapLatest {
        if (it) UiSurface.LOCKSCREEN else UiSurface.HOMESCREEN
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, UiSurface.HOMESCREEN)

    private val isDarkMode = uiSurface.flatMapLatest {
        when(it) {
            UiSurface.HOMESCREEN -> wallpaperRepository.homescreenWallpaperDarkTextColour
            UiSurface.LOCKSCREEN -> wallpaperRepository.lockscreenWallpaperDarkTextColour
            UiSurface.MEDIA_DATA_MANAGER -> wallpaperRepository.homescreenWallpaperDarkTextColour
            UiSurface.GLANCEABLE_HUB -> wallpaperRepository.lockscreenWallpaperDarkTextColour
        }
    }

    private val isSystemDarkMode = resumeBus.mapLatest {
        context.isDarkMode
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, context.isDarkMode)

    private val isDark = combine(
        isDarkMode.filterNotNull(),
        isSystemDarkMode,
        settingsRepository.expandedBackground.asFlow()
    ) { dark, systemDark, background ->
        if (background == ExpandedBackground.SOLID) !systemDark else dark
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, context.isDarkMode)

    // Use emptyList() as initial value so expandedWidgets can emit immediately on startup
    // rather than waiting for the DB query to complete and unblocking State.Loaded sooner.
    private val expandedWidgetConfigs = expandedRepository.getExpandedAppWidgets()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, emptyList())

    private var appShortcuts: List<ShortcutInfo>? = null

    // Search box has been removed from the expanded space UI; userSettings no longer needs
    // expandedSearchApp (PackageManager load) or getBitmap() (Glide fetch) in the critical path.
    // Initial values use getSync() (reads the already-loaded StateFlow value) so the initial
    // ExpandedSettings matches the real settings, preventing a filterDistinct-passing re-emission
    // when the stateIn coroutine first delivers real values on Main.
    private val userSettings = combine(
        settingsRepository.enhancedMode.asFlow(),
        isDark,
        settingsRepository.expandedShowWeatherCookie.asFlow()
    ) { enhanced, dark, cookie ->
        ExpandedSettings(enhanced, dark, cookie)
    }.stateIn(
        lifecycleScope,
        SharingStarted.Eagerly,
        ExpandedSettings(
            settingsRepository.enhancedMode.getSync(),
            context.isDarkMode,
            settingsRepository.expandedShowWeatherCookie.getSync()
        )
    )

    // getSync() reads the pre-loaded StateFlow value synchronously so the initial ExpandedViewState
    // matches the real hasClickedAdd value, preventing an unnecessary convert() re-emission.
    private val viewState = combine(
        topInset,
        settings.expandedHasClickedAdd.asFlow(),
        resumeBus
    ) { inset, hasClickedAdd, _ ->
        ExpandedViewState(inset, hasClickedAdd)
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, ExpandedViewState(0, settings.expandedHasClickedAdd.getSync()))

    // Pre-seeded with emptyList() so the combine below can emit immediately on startup.
    private val customWidgetsDb = expandedRepository.expandedCustomAppWidgets
        .stateIn(lifecycleScope, SharingStarted.Eagerly, emptyList())

    // flowOn(IO) is critical: getProviders() queries AppWidgetManager/PackageManager, which
    // can block for seconds on a cold start. Previously this was protected by convert().flowOn(IO),
    // but now that expandedWidgets uses stateIn(Eagerly), it's collected from lifecycleScope
    // (Main) — so we must explicitly move this lambda onto IO.
    private val expandedCustomAppWidgets = combine(
        customWidgetsDb,
        uiSurface,
        isDark
    ) { widgets, surface, dark ->
        val providers = widgetRepository.getProviders()
        widgets.mapNotNull { widget ->
            if(!widget.showWhenLocked && surface != UiSurface.HOMESCREEN){
                return@mapNotNull null
            }
            val provider = providers.firstOrNull { info ->
                info.provider == ComponentName.unflattenFromString(widget.provider)
            } ?: return@mapNotNull Item.RemovedWidget(widget.appWidgetId, dark)
            val config = CustomExpandedAppWidgetConfig(
                widget.spanX,
                widget.spanY,
                widget.index,
                widget.showWhenLocked,
                widget.roundCorners,
                widget.fullWidth
            )
            Item.Widget(
                provider,
                widget.appWidgetId,
                widget.id,
                null,
                null,
                true,
                false,
                config,
                widget.spanX,
                widget.spanY,
                widget.fullWidth,
                widget.roundCorners,
                dark
            )
        }
    }.flowOn(Dispatchers.IO)

    // Pre-seeded so convert() is not blocked by expandedCustomAppWidgets IO dispatches.
    private val expandedWidgets = combine(
        expandedWidgetConfigs,
        expandedCustomAppWidgets
    ) { ids, custom ->
        Pair(ids, custom)
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, Pair(emptyList<ExpandedAppWidget>(), emptyList<Item>()))

    override suspend fun collectInto(id: SmartspaceSessionId, targets: List<Item>) {
        collectInto(targets)
    }

    override fun convert(
        pages: Flow<List<SmartspacePageHolder>>,
        uiSurface: Flow<UiSurface>
    ): Flow<List<Item>> {
        return combine(
            pages,
            uiSurface,
            expandedWidgets,
            userSettings,
            viewState
        ) { p, s, w, u, v ->
            p.toItems(s, w.first, w.second, u, v)
        }.flowOn(Dispatchers.IO)
    }

    private fun SmartspaceTarget.addFakeWidgetIfNeeded() = apply {
        remoteViews?.let {
            if(widget != null) return@let
            widget = context.createFakeWidgetProviderInfo()
        }
    }

    override fun applyActionOverrides(targets: Flow<List<TargetHolder>>): Flow<List<TargetHolder>> {
        return targets //Don't override actions in expanded mode
    }

    fun setTopInset(inset: Int) = whenCreated {
        topInset.emit(inset)
    }

    override fun onResume() {
        super.onResume()
        whenCreated {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    fun onDeleteCustomWidget(appWidgetId: Int) = whenCreated {
        databaseRepository.deleteExpandedCustomAppWidget(appWidgetId)
        widgetRepository.deallocateAppWidgetId(appWidgetId)
    }

    private suspend fun List<SmartspacePageHolder>.toItems(
        surface: UiSurface,
        widgets: List<ExpandedAppWidget>,
        customWidgets: List<Item>,
        settings: ExpandedSettings,
        viewState: ExpandedViewState
    ): List<Item> {
        // When the weather cookie is enabled (default), strip all weather targets so they
        // are excluded from rawPageTargets and the complications row — weather is shown
        // exclusively as the circular cookie badge.
        // When the cookie is disabled, keep weather targets in the list so they appear
        // as regular pages in the header pill.
        val finalList = if (settings.showWeatherCookie) {
            val w = SmartspaceTarget.FEATURE_WEATHER
            // IDs of the actions that feed the weather cookie — the most reliable identifier
            val weatherActionIds = smartspaceRepository.getDefaultHomeActions().value
                .map { it.id }.toSet()
            fun isCurrentWeatherAction(id: String?) = id != null && weatherActionIds.contains(id)
            mapNotNull { holder ->
                val t = holder.page
                val headerIsWeather = isCurrentWeatherAction(t.headerAction?.id)
                val baseIsWeather = isCurrentWeatherAction(t.baseAction?.id)
                when {
                    // Regular FEATURE_WEATHER targets — drop entirely
                    t.featureType == w -> null
                    // Blank complication targets with weather in one or both slots.
                    t.smartspaceTargetId.startsWith(BLANK_TARGET_PREFIX) &&
                    (headerIsWeather || baseIsWeather) -> {
                        val td = t.templateData
                        when {
                            headerIsWeather && baseIsWeather -> null
                            // Header is weather but base has a complication — promote base to header
                            headerIsWeather -> holder.copy(page = t.copy(
                                headerAction = t.baseAction,
                                baseAction = null,
                                templateData = td?.copy(
                                    subtitleItem = td.subtitleSupplementalItem,
                                    subtitleSupplementalItem = null
                                ),
                                remoteViews = null
                            ))
                            // Base is weather — null it out, keep header
                            else -> holder.copy(page = t.copy(
                                baseAction = null,
                                templateData = td?.copy(subtitleSupplementalItem = null),
                                remoteViews = null
                            ))
                        }
                    }
                    // Non-blank target with weather as a subtitle action — strip it.
                    // Promote supplemental into subtitle if present, otherwise just null it.
                    headerIsWeather || baseIsWeather -> {
                        val td = t.templateData
                        holder.copy(page = t.copy(
                            headerAction = if (headerIsWeather) null else t.headerAction,
                            baseAction = if (baseIsWeather) null else t.baseAction,
                            templateData = td?.copy(
                                subtitleItem = if (headerIsWeather) td.subtitleSupplementalItem
                                              else td.subtitleItem,
                                subtitleSupplementalItem = if (headerIsWeather || baseIsWeather) null
                                                           else td.subtitleSupplementalItem
                            ),
                            remoteViews = null
                        ))
                    }
                    else -> holder
                }
            }
        } else {
            // Cookie disabled — keep all targets including weather
            toList()
        }

        rawPageTargets.value = finalList.map { it.page }
        val isDark = settings.isDark
        val list = ArrayList<Item>()
        list.add(Item.StatusBarSpace(viewState.topInset, isDark))
        val splitLists = finalList.split {
            it.page.smartspaceTargetId.startsWith(BLANK_TARGET_PREFIX)
        }

        // Drop blank targets that became empty after weather stripping.
        val complications = splitLists.second
            .filter { it.page.templateData?.subtitleItem != null }
            .map { it.page }
        splitLists.first.forEach {
            val extras = it.getExtras(
                surface,
                widgets,
                settings.enhancedEnabled,
                isDark,
                false
            )
            list.add(Item.Target(
                it.page.addFakeWidgetIfNeeded(),
                it.target?.id, extras.isNotEmpty(),
                true,
                isDark
            ))
            //Force a new line if any extras are being shown alongside this Target
            if(extras.isNotEmpty()) {
                list.add(Item.Spacer(isDark))
            }
            list.addAll(extras)
            if(extras.isNotEmpty()) {
                list.add(Item.Spacer(isDark))
            }
        }
        if(complications.isNotEmpty()) {
            list.add(Item.Complications(complications.extractComplications(), true, isDark))
        }
        list.add(Item.Spacer(isDark))
        list.addAll(customWidgets)
        list.add(Item.Footer(viewState.hasClickedAdd, isDark))
        return list
    }

    private fun List<SmartspaceTarget>.extractComplications(): ExpandedComplications {
        return map { target ->
            val header = target.templateData?.subtitleItem?.let {
                Complication.SubItemInfo(target, it)
            } ?: target.headerAction?.let {
                Complication.Action(target, it)
            }
            val base = target.templateData?.subtitleSupplementalItem?.let {
                Complication.SubItemInfo(target, it)
            } ?: target.baseAction?.takeUnless {
                target.smartspaceTargetId.startsWith(BLANK_TARGET_PREFIX)
            }?.let {
                Complication.Action(target, it)
            }
            listOfNotNull(header, base)
        }.flatten().filter {
            it.isValid()
        }.let {
            ExpandedComplications(it)
        }
    }

    private suspend fun SmartspacePageHolder?.getExtras(
        surface: UiSurface,
        widgets: List<ExpandedAppWidget>,
        enhanced: Boolean,
        isDark: Boolean,
        useGoogleSans: Boolean
    ): List<Item> {
        if(this == null || target == null) return emptyList()
        val isLocked = surface == UiSurface.LOCKSCREEN
        val items = ArrayList<Item>()
        val config = target.config
        val expanded = page.expandedState
        val widget = expanded?.widget
        val appShortcuts = expanded?.appShortcuts
        val shouldLoadAppShortcuts = config.showAppShortcuts && enhanced && appShortcuts != null
                && (appShortcuts.showWhenLocked || !isLocked)
        if(config.showWidget && widget != null && (widget.showWhenLocked || !isLocked)){
            val id = widgets.firstOrNull {
                it.componentName == widget.info.provider.flattenToString() && it.id == widget.id
            }?.appWidgetId
            @Suppress("CloseWidget")
            items.add(Item.Widget(
                widget.info,
                id,
                widget.id,
                widget.width,
                widget.height,
                isCustom = false,
                config = null,
                useGoogleSans = useGoogleSans,
                spanX = null,
                spanY = null,
                fullWidth = false,
                roundCorners = true,
                isDark = isDark
            ))
        }
        val remoteViews = expanded?.remoteViews
        if(config.showRemoteViews && remoteViews != null) {
            val views = if(isLocked){
                remoteViews.locked
            }else{
                remoteViews.unlocked
            }
            if(views != null){
                items.add(Item.RemoteViews(views, page.smartspaceTargetId, isDark))
            }
        }
        val shortcuts = expanded?.shortcuts?.shortcuts
        val shortcutsToShow = ArrayList<BaseShortcut>()
        if(shouldLoadAppShortcuts && appShortcuts != null){
            shortcutsToShow.addAll(appShortcuts.getAppShortcuts())
        }
        if(config.showShortcuts && shortcuts != null){
            val filteredShortcuts = shortcuts.filter {
                it.showWhenLocked || !isLocked
            }.take(MAX_SHORTCUTS)
            if(filteredShortcuts.isNotEmpty()) {
                shortcutsToShow.addAll(filteredShortcuts)
            }
        }
        if(shortcutsToShow.isNotEmpty()){
            items.add(Item.Shortcuts(shortcutsToShow, page.smartspaceTargetId, isDark))
        }
        return items
    }

    private suspend fun loadAppShortcuts(): List<ShortcutInfo>? {
        if(appShortcuts != null) return appShortcuts
        val appShortcuts = try {
            shizukuServiceRepository.runWithService {
                it.getShortcuts(ShortcutQueryWrapper(
                    FLAG_MATCH_DYNAMIC or FLAG_MATCH_MANIFEST
                ))
            }.unwrap() as? ParceledListSlice<ShortcutInfo>
        }catch (e: SecurityException){
            return emptyList()
        }
        this.appShortcuts = appShortcuts?.list
        return this.appShortcuts
    }

    /**
     *  Progressively loads app shortcuts from the full list, until either none are left which match
     *  the criteria (specified package names) or the limit ([MAX_SHORTCUTS]) is reached.
     */
    private suspend fun ExpandedState.AppShortcuts.getAppShortcuts(): List<AppShortcut> {
        val limit = appShortcutCount.coerceAtMost(MAX_SHORTCUTS)
        val appShortcuts = loadAppShortcuts()?.groupBy {
            it.`package`
        }?.filter {
            packageNames.contains(it.key)
        }?.mapValues {
            it.value.sortedBy { shortcut -> shortcut.rank }
        }?.map {
            LinkedList(it.value)
        } ?: return emptyList()
        val shortcuts = ArrayList<ShortcutInfo>()
        while(!appShortcuts.all { it.isEmpty() }) {
            appShortcuts.forEach {
                val shortcut = it.pop()
                if(shortcuts.size > limit) return@forEach
                shortcuts.add(shortcut)
            }
        }
        return shortcuts.mapNotNull {
            it.getAppShortcut()
        }
    }

    /**
     *  Loads a given [ShortcutInfo]'s icon into the [AppShortcut] model
     */
    private suspend fun ShortcutInfo.getAppShortcut(): AppShortcut? {
        val icon = shizukuServiceRepository.runWithService {
            try {
                it.getAppShortcutIcon(`package`, id)
            }catch (e: DeadObjectException){
                //Service died
                null
            }
        }.unwrap() ?: return null
        return AppShortcut(shortLabel ?: longLabel ?: id, icon, `package`, id)
    }

    override fun toSmartspacerSessionId(id: SmartspaceSessionId): SmartspaceSessionId {
        return id
    }

    override suspend fun supportsRemoteViews() = true

    sealed class Item(val type: Type, open val isDark: Boolean) {
        data class StatusBarSpace(
            val topInset: Int,
            override val isDark: Boolean
        ): Item(Type.STATUS_BAR_SPACE, isDark)

        data class Search(
            val doodleImage: DoodleImage?,
            val searchApp: SearchApp?,
            val searchBackgroundColor: Int?,
            val isLightStatusBar: Boolean,
            val topInset: Int = 0,
            override val isDark: Boolean
        ): Item(Type.SEARCH, isDark)

        data class Target(
            val target: SmartspaceTarget,
            val parentId: String?,
            val hasExtras: Boolean,
            val applyShadow: Boolean,
            override val isDark: Boolean
        ): Item(Type.TARGET, isDark) {
            override fun getStaticId() = "${parentId ?: "default"}_${target.smartspaceTargetId}"
        }

        data class Complications(
            val complications: ExpandedComplications,
            val showShadow: Boolean,
            override val isDark: Boolean
        ): Item(Type.COMPLICATIONS, isDark)

        data class Widget(
            val provider: AppWidgetProviderInfo,
            val appWidgetId: Int?,
            val id: String?,
            val width: Int?,
            val height: Int?,
            val isCustom: Boolean,
            val useGoogleSans: Boolean,
            val config: CustomExpandedAppWidgetConfig?,
            val spanX: Int?,
            val spanY: Int?,
            val fullWidth: Boolean,
            val roundCorners: Boolean,
            override val isDark: Boolean
        ): Item(Type.WIDGET, isDark) {
            override fun getStaticId() = "widget_$appWidgetId"

            override fun equals(other: Any?): Boolean {
                if(other !is Widget) return false
                if(other.provider.provider != provider.provider) return false
                if(other.appWidgetId != appWidgetId) return false
                if(other.id != id) return false
                if(other.width != width) return false
                if(other.height != height) return false
                if(other.isCustom != isCustom) return false
                if(other.useGoogleSans != useGoogleSans) return false
                if(other.config != config) return false
                if(other.isDark != isDark) return false
                if(other.fullWidth != fullWidth) return false
                if(other.roundCorners != roundCorners) return false
                if(other.spanX != spanX) return false
                if(other.spanY != spanY) return false
                return true
            }

            override fun hashCode(): Int {
                var result = provider.hashCode()
                result = 31 * result + (appWidgetId ?: 0)
                result = 31 * result + (id?.hashCode() ?: 0)
                result = 31 * result + (width ?: 0)
                result = 31 * result + (height ?: 0)
                result = 31 * result + isCustom.hashCode()
                result = 31 * result + useGoogleSans.hashCode()
                result = 31 * result + (config?.hashCode() ?: 0)
                result = 31 * result + isDark.hashCode()
                result = 31 * result + fullWidth.hashCode()
                result = 31 * result + roundCorners.hashCode()
                result = 31 * result + (spanX?.hashCode() ?: 0)
                result = 31 * result + (spanY?.hashCode() ?: 0)
                return result
            }
        }

        data class RemovedWidget(
            val appWidgetId: Int?,
            override val isDark: Boolean
        ): Item(Type.REMOVED_WIDGET, isDark) {
            override fun getStaticId() = "removed_widget_$appWidgetId"
        }

        data class RemoteViews(
            val remoteViews: android.widget.RemoteViews,
            val parentId: String,
            override val isDark: Boolean
        ): Item(Type.REMOTE_VIEWS, isDark) {
            override fun getStaticId() = "remote_views_$parentId"
        }

        data class Shortcuts(
            val shortcuts: List<BaseShortcut>,
            val parentId: String,
            override val isDark: Boolean
        ): Item(Type.SHORTCUTS, isDark) {
            override fun getStaticId() = "shortcuts_$parentId"
        }

        data class Footer(
            val hasClickedAdd: Boolean,
            override val isDark: Boolean
        ): Item(Type.FOOTER, isDark)

        data class Spacer(override val isDark: Boolean): Item(Type.SPACER, isDark)

        open fun getStaticId(): String = type.name

        enum class Type {
            STATUS_BAR_SPACE,
            SEARCH,
            TARGET,
            COMPLICATIONS,
            WIDGET,
            REMOVED_WIDGET,
            REMOTE_VIEWS,
            SHORTCUTS,
            FOOTER,
            SPACER
        }
    }

    data class ExpandedSettings(
        val enhancedEnabled: Boolean,
        val isDark: Boolean,
        val showWeatherCookie: Boolean
    )

    data class ExpandedViewState(
        val topInset: Int,
        val hasClickedAdd: Boolean
    )

    init {
        onCreate()
    }

}