package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.permission.client.SmartspacerClientPermissionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject
import android.graphics.drawable.Icon as AndroidIcon

class ClientSmartspacerSession(
    private val context: Context,
    config: SmartspaceConfig,
    override val sessionId: SmartspaceSessionId,
    private val collectIntoExt: suspend (SmartspaceSessionId, List<SmartspaceTarget>) -> Unit
): BaseSmartspacerSession<SmartspaceTarget, SmartspaceSessionId>(context, config, sessionId) {

    private val databaseRepository by inject<DatabaseRepository>()

    var lastTargets: List<SmartspaceTarget>? = null

    private val permissions = databaseRepository.getGrants().map {
        it.firstOrNull { grant -> grant.packageName == config.packageName }
            ?: Grant(config.packageName)
    }.flowOn(Dispatchers.IO).stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    override val targetCount = flowOf(config.smartspaceTargetCount)

    override suspend fun collectInto(id: SmartspaceSessionId, targets: List<SmartspaceTarget>) {
        collectIntoExt(id, targets).also { lastTargets = targets }
    }

    override fun convert(
        pages: Flow<List<SmartspaceRepository.SmartspacePageHolder>>,
        uiSurface: Flow<UiSurface>
    ): Flow<List<SmartspaceTarget>> {
        return combine(
            pages,
            permissions.filterNotNull()
        ) { targets, grant ->
            if(grant.smartspace) {
                targets.map { it.page }
            }else{
                listOf(createPermissionTarget(grant))
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun createPermissionTarget(grant: Grant): SmartspaceTarget = TargetTemplate.Basic(
        id = "permission_request",
        componentName = ComponentName(BuildConfig.APPLICATION_ID, "permission_request"),
        title = Text(context.getString(R.string.target_permission_request_title)),
        subtitle = Text(context.getString(R.string.target_permission_request_content)),
        icon = Icon(AndroidIcon.createWithResource(context, R.drawable.ic_notification)),
        onClick = TapAction(
            pendingIntent = SmartspacerClientPermissionActivity.createPendingIntent(context, grant)
        )
    ).create()

    override fun toSmartspacerSessionId(id: SmartspaceSessionId) = id

    init {
        onCreate()
    }

}