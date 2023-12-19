package com.kieronquinn.app.smartspacer.components.smartspace

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.components.smartspace.WidgetSmartspacerSessionState.DotConfig
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Companion.FEATURE_ALLOWLIST_DOORBELL
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_FRAME_DURATION_MS
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.DoorbellState
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Images.Companion.GIF_FRAME_DURATION_MS
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView.ViewType
import com.kieronquinn.app.smartspacer.ui.views.smartspace.features.DoorbellFeatureSmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.features.WeatherFeatureSmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.templates.ImagesTemplateSmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.math.min

open class WidgetSmartspacerSession(
    context: Context,
    widget: AppWidget,
    private val config: SmartspaceConfig = widget.getConfig(),
    private val collectInto: suspend (AppWidget) -> Unit
): BaseSmartspacerSession<SmartspaceView, AppWidget>(context, config, widget) {

    companion object {
        internal fun AppWidget.getConfig(): SmartspaceConfig {
            return SmartspaceConfig(1, surface, ownerPackage)
        }

        private const val DEFAULT_FRAME_DELAY = 1000L
        private const val MINIMUM_FRAME_DELAY = 500L
    }

    open val includeBasic: Boolean = false

    val appWidgetId = widget.appWidgetId
    val packageName = widget.ownerPackage
    val surface = widget.surface
    var state: WidgetSmartspacerSessionState? = null

    private val isResumed = MutableStateFlow(false)
    private val index = MutableStateFlow(0)
    private var visibleTargetId: String? = null
    private var pageCount: Int = 0
    private val pageChangeLock = Mutex()
    private val multiPage = widget.multiPage
    private val showControls = widget.showControls
    private val animate = widget.animate

    override val targetCount = if(widget.multiPage) {
        flowOf(Integer.MAX_VALUE)
    }else{
        //Without controls, lock the widget to single page
        flowOf(1)
    }

    override suspend fun collectInto(id: AppWidget, targets: List<SmartspaceView>) {
        collectInto.invoke(id)
    }

    override fun convert(
        pages: Flow<List<SmartspacePageHolder>>,
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
            state = WidgetSmartspacerSessionState(
                it.second,
                config,
                shouldAnimate,
                isFirst,
                isLast,
                isOnlyPage,
                showControls,
                getDotConfig()
            )
            listOf(it.first.view)
        }
    }

    private fun SmartspacePageHolder.toSmartspaceViewFlow(): Flow<Pair<SmartspacerViewState, WidgetSmartspacerPage>> {
        visibleTargetId = page.smartspaceTargetId
        val state = toSmartspaceViewState()
        return state.map {
            val page = WidgetSmartspacerPage(this, it.view.viewType, it.view, it.basicView)
            Pair(it, page)
        }
    }

    override fun toSmartspacerSessionId(id: AppWidget): SmartspaceSessionId {
        return SmartspaceSessionId(
            "${id.ownerPackage}:widget:${id.appWidgetId}",
            android.os.Process.myUserHandle()
        )
    }

    private fun SmartspacePageHolder.toSmartspaceViewState(): Flow<SmartspacerViewState> {
        val template = page.templateData
        val basic = if(includeBasic) {
            SmartspaceView.fromTarget(page, config.uiSurface, true)
        }else null
        return when {
            !includeBasic && template is SubImageTemplateData -> {
                page.startLoopImages(page.smartspaceTargetId, template).map {
                    SmartspacerViewState(it, null, basic)
                }
            }
            !includeBasic && page.isDoorbellTargetWithUris() -> {
                page.startLoopDoorbellImages().map {
                    SmartspacerViewState(it, null, basic)
                }
            }
            else -> {
                val page = SmartspaceView.fromTarget(page, config.uiSurface, false)
                flowOf(page).map {
                    SmartspacerViewState(it, this.page, basic)
                }
            }
        }
    }

    private fun SmartspaceTarget.startLoopImages(
        targetId: String,
        template: SubImageTemplateData
    ) = isResumed.takeWhile {
        visibleTargetId == smartspaceTargetId
    }.flatMapLatest {
        if(it){
            loopImages(targetId, template)
                .takeWhile { isResumed.value && visibleTargetId == smartspaceTargetId }
        }else{
            flowOf(ImagesTemplateSmartspaceView(targetId, this, template, config.uiSurface))
        }
    }

    @SuppressLint("InlinedApi")
    private fun SmartspaceTarget.loopImages(
        targetId: String,
        template: SubImageTemplateData
    ): Flow<SmartspaceView> = flow {
        val requestedDelay = template.subImageAction?.extras?.getInt(GIF_FRAME_DURATION_MS)?.toLong()
            ?: DEFAULT_FRAME_DELAY
        val delay = requestedDelay.coerceAtLeast(MINIMUM_FRAME_DELAY)
        loopWithDelay(requestedDelay, delay, template.subImages.size){ count ->
            val icon = template.subImages[count]
            val newTemplate = template.copy(subImages = listOf(icon))
            emit(ImagesTemplateSmartspaceView(
                targetId, this@loopImages, newTemplate, config.uiSurface
            ))
        }
    }

    private fun SmartspaceTarget.isDoorbellTargetWithUris(): Boolean {
        if(!FEATURE_ALLOWLIST_DOORBELL.contains(featureType)) return false
        if(DoorbellState.fromTarget(this) !is DoorbellState.ImageUri) return false
        return true
    }

    private fun SmartspaceTarget.startLoopDoorbellImages() = isResumed.takeWhile {
        visibleTargetId == smartspaceTargetId
    }.flatMapLatest {
        if(it){
            loopDoorbell().takeWhile { isResumed.value && visibleTargetId == smartspaceTargetId }
        }else{
            flowOf(DoorbellFeatureSmartspaceView(smartspaceTargetId, this, surface))
        }
    }

    private fun SmartspaceTarget.loopDoorbell(): Flow<SmartspaceView> = flow {
        val requestedDelay = baseAction?.extras?.getInt(KEY_FRAME_DURATION_MS)?.toLong()
            ?: DEFAULT_FRAME_DELAY
        val delay = requestedDelay.coerceAtLeast(MINIMUM_FRAME_DELAY)
        loopWithDelay(requestedDelay, delay, iconGrid.size){ count ->
            val icon = iconGrid[count]
            val target = copy(iconGrid = listOf(icon))
            emit(DoorbellFeatureSmartspaceView(smartspaceTargetId, target, surface))
        }
    }

    private suspend fun loopWithDelay(
        requestedDelay: Long,
        delay: Long,
        itemCount: Int,
        block: suspend (count: Int) -> Unit
    ) {
        while(currentCoroutineContext().isActive){
            val start = System.currentTimeMillis()
            var count = 0
            while(count < itemCount - 1){
                delay(delay)
                val now = System.currentTimeMillis()
                val elapsed = now - start
                val adjustedCount = (elapsed / requestedDelay).toInt()
                    .coerceAtMost(itemCount - 1)
                block(adjustedCount)
                count = adjustedCount
            }
        }
    }

    override fun onPause() {
        lifecycleScope.launch {
            isResumed.emit(false)
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            isResumed.emit(true)
        }
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
            SmartspacePageHolder(target, null, emptyList()),
            ViewType.FEATURE_WEATHER,
            WeatherFeatureSmartspaceView(target.smartspaceTargetId, target, surface),
            basic
        )
    }

    private fun createEmptyTarget(): SmartspaceTarget {
        return SmartspaceTarget(
            smartspaceTargetId = UUID.randomUUID().toString(),
            featureType = SmartspaceTarget.FEATURE_WEATHER,
            componentName = ComponentName("package_name", "class_name"),
            canBeDismissed = false
        )
    }

    init {
        onCreate()
    }

    private data class SmartspacerViewState(
        val view: SmartspaceView,
        val target: SmartspaceTarget?,
        val basicView: SmartspaceView?
    )

}

data class WidgetSmartspacerPage(
    val holder: SmartspacePageHolder,
    val type: ViewType,
    val view: SmartspaceView,
    val basicView: SmartspaceView?
)

data class WidgetSmartspacerSessionState(
    val page: WidgetSmartspacerPage,
    val config: SmartspaceConfig,
    val animate: Boolean,
    val isFirst: Boolean,
    val isLast: Boolean,
    val isOnlyPage: Boolean,
    val showControls: Boolean,
    val dotConfig: List<DotConfig>
) {

    enum class DotConfig {
        REGULAR, HIGHLIGHTED
    }

}