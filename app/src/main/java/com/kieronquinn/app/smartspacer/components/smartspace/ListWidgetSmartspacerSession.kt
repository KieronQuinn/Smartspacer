package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView.ViewType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class ListWidgetSmartspacerSession(
    context: Context,
    private val widget: AppWidget,
    private val config: SmartspaceConfig = widget.getConfig(),
    collectInto: suspend (AppWidget) -> Unit
): WidgetSmartspacerSession(context, widget, config, collectInto) {

    var state: ListWidgetSmartspacerSessionState? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun convert(
        pages: Flow<List<SmartspacePageHolder>>,
        uiSurface: Flow<UiSurface>
    ): Flow<List<SmartspaceView>> {
        return combine(pages, uiSurface) { it, _ ->
            it.ifEmpty {
                listOf(SmartspacePageHolder(createEmptyTarget(), null, emptyList()))
            }.map {
                it.toSmartspaceViewFlow()
            }
        }.flatMapLatest {
            combine(*it.toTypedArray()) { pages ->
                pages.map { page -> page.second }
            }
        }.flowOn(Dispatchers.IO).distinctUntilChanged { old, new ->
            if(old.size != new.size) return@distinctUntilChanged false
            if(new.isEmpty()) return@distinctUntilChanged true
            val zipped = old.zip(new)
            zipped.all {
                it.first.equalsForUi(it.second)
            }
        }.onEach {
            state = ListWidgetSmartspacerSessionState(it, config, widget)
        }.map {
            it.map { page -> page.view }
        }
    }

    private fun WidgetSmartspacerPage.equalsForUi(other: WidgetSmartspacerPage): Boolean {
        if(this.type.isDynamic() || other.type.isDynamic()) return false //Dynamic = always change
        return this.holder.page.equalsForUi(other.holder.page)
    }

    private fun ViewType.isDynamic(): Boolean {
        return when(this) {
            ViewType.TEMPLATE_IMAGES, ViewType.FEATURE_DOORBELL -> true
            else -> false
        }
    }

    override fun setVisibleTarget(targetId: String) {
        //No-op for list since all targets are "visible"
    }

    override fun isTargetVisible(targetId: String): Boolean = true

    init {
        onCreate()
    }

}

data class ListWidgetSmartspacerSessionState(
    val pages: List<WidgetSmartspacerPage>,
    val config: SmartspaceConfig,
    val widgetConfig: AppWidget
)