package com.kieronquinn.app.smartspacer.components.smartspace

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Companion.FEATURE_ALLOWLIST_DOORBELL
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_FRAME_DURATION_MS
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.DoorbellState
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Images.Companion.GIF_FRAME_DURATION_MS
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.features.DoorbellFeatureSmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.templates.ImagesTemplateSmartspaceView
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

abstract class WidgetSmartspacerSession(
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

    private val isResumed = MutableStateFlow(false)

    override val targetCount = if(widget.multiPage) {
        flowOf(Integer.MAX_VALUE)
    }else{
        //Without controls, lock the widget to single page
        flowOf(1)
    }

    override suspend fun collectInto(id: AppWidget, targets: List<SmartspaceView>) {
        collectInto.invoke(id)
    }

    abstract fun setVisibleTarget(targetId: String)
    abstract fun isTargetVisible(targetId: String): Boolean

    override fun toSmartspacerSessionId(id: AppWidget): SmartspaceSessionId {
        return SmartspaceSessionId(
            "${id.ownerPackage}:widget:${id.appWidgetId}",
            android.os.Process.myUserHandle()
        )
    }

    protected fun SmartspacePageHolder.toSmartspaceViewFlow(): Flow<Pair<SmartspacerViewState, WidgetSmartspacerPage>> {
        setVisibleTarget(page.smartspaceTargetId)
        val state = toSmartspaceViewState()
        return state.map {
            val page = WidgetSmartspacerPage(this, it.view.viewType, it.view, it.basicView)
            Pair(it, page)
        }
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
        isTargetVisible(smartspaceTargetId)
    }.flatMapLatest {
        if(it){
            loopImages(targetId, template)
                .takeWhile { isResumed.value && isTargetVisible(smartspaceTargetId) }
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
        isTargetVisible(smartspaceTargetId)
    }.flatMapLatest {
        if(it){
            loopDoorbell().takeWhile { isResumed.value && isTargetVisible(smartspaceTargetId) }
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

    protected fun createEmptyTarget(): SmartspaceTarget {
        return SmartspaceTarget(
            smartspaceTargetId = UUID.randomUUID().toString(),
            featureType = SmartspaceTarget.FEATURE_WEATHER,
            componentName = ComponentName("package_name", "class_name"),
            canBeDismissed = false
        )
    }

    protected data class SmartspacerViewState(
        val view: SmartspaceView,
        val target: SmartspaceTarget?,
        val basicView: SmartspaceView?
    )

}

data class WidgetSmartspacerPage(
    val holder: SmartspacePageHolder,
    val type: SmartspaceView.ViewType,
    val view: SmartspaceView,
    val basicView: SmartspaceView?
)