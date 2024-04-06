package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSessionState.DotConfig
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.features.WeatherFeatureSmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

open class PagedWidgetSmartspacerSession(
    context: Context,
    widget: AppWidget,
    private val config: SmartspaceConfig = widget.getConfig(),
    collectInto: suspend (AppWidget) -> Unit
): WidgetSmartspacerSession(context, widget, config, collectInto) {

    private val index = MutableStateFlow(0)
    private var visibleTargetId: String? = null
    private var pageCount: Int = 0
    private val pageChangeLock = Mutex()
    private val multiPage = widget.multiPage
    private val showControls = widget.showControls
    private val invisibleControls = widget.hideControls
    private val animate = widget.animate

    var state: PagedWidgetSmartspacerSessionState? = null

    override fun convert(
        pages: Flow<List<SmartspaceRepository.SmartspacePageHolder>>,
        uiSurface: Flow<UiSurface>
    ): Flow<List<SmartspaceView>> {
        var id: String? = null
        var previousPageCount = -1
        return combine(pages, index, uiSurface){ it, i, _ ->
            previousPageCount = pageCount
            pageCount = it.size
            var index = min(i, it.size)
            val page = it.getOrNull(index) ?: run {
                index = it.size - 1
                it.getOrNull(index)
            } ?: createDatePage().holder
            page.toSmartspaceViewFlow()
        }.flowOn(Dispatchers.IO).flattenMerge().distinctUntilChanged { oldPair, newPair ->
            val old = oldPair.first
            val new = newPair.first
            previousPageCount == pageCount && old.target != null && new.target != null
                    && old.target.equalsForUi(new.target)
        }.map {
            val shouldAnimate = animate && it.first.target != null
                    && it.first.target?.smartspaceTargetId != id
            id = it.first.target?.smartspaceTargetId
            val index = index.value
            val isFirst = index == 0
            val isLast = index >= pageCount - 1
            val isOnlyPage = pageCount == 1 || !multiPage
            state = PagedWidgetSmartspacerSessionState(
                it.second,
                config,
                shouldAnimate,
                isFirst,
                isLast,
                isOnlyPage,
                showControls,
                invisibleControls,
                getDotConfig()
            )
            listOf(it.first.view)
        }
    }

    override fun setVisibleTarget(targetId: String) {
        visibleTargetId = targetId
    }

    override fun isTargetVisible(targetId: String): Boolean {
        return visibleTargetId == targetId
    }

    fun nextPage() = whenCreated {
        pageChangeLock.withLock {
            val currentIndex = normaliseIndex(index.value)
            var newIndex = currentIndex + 1
            if(newIndex >= pageCount) newIndex = 0
            index.emit(newIndex)
        }
    }

    fun previousPage() = whenCreated {
        pageChangeLock.withLock {
            val currentIndex = normaliseIndex(index.value)
            val newIndex = (currentIndex - 1).coerceAtLeast( 0)
            index.emit(newIndex)
        }
    }

    private fun normaliseIndex(index: Int): Int {
        return index.coerceAtLeast(0).coerceAtMost(pageCount - 1)
    }

    private fun getDotConfig(): List<DotConfig> {
        val dots = ArrayList<DotConfig>()
        val index = index.value
        for(i in 0 until pageCount) {
            if(i == index) {
                dots.add(DotConfig.HIGHLIGHTED)
            }else{
                dots.add(DotConfig.REGULAR)
            }
        }
        return dots
    }

    private fun createDatePage(): WidgetSmartspacerPage {
        val target = createEmptyTarget()
        val basic = if(includeBasic) {
            WeatherFeatureSmartspaceView(target.smartspaceTargetId, target, surface)
        }else null
        return WidgetSmartspacerPage(
            SmartspaceRepository.SmartspacePageHolder(target, null, emptyList()),
            SmartspaceView.ViewType.FEATURE_WEATHER,
            WeatherFeatureSmartspaceView(target.smartspaceTargetId, target, surface),
            basic
        )
    }

    init {
        onCreate()
    }

}

data class PagedWidgetSmartspacerSessionState(
    val page: WidgetSmartspacerPage,
    val config: SmartspaceConfig,
    val animate: Boolean,
    val isFirst: Boolean,
    val isLast: Boolean,
    val isOnlyPage: Boolean,
    val showControls: Boolean,
    val invisibleControls: Boolean,
    val dotConfig: List<DotConfig>
) {

    enum class DotConfig {
        REGULAR, HIGHLIGHTED
    }

}