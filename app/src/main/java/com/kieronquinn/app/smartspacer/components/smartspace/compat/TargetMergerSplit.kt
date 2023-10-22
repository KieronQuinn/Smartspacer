package com.kieronquinn.app.smartspacer.components.smartspace.compat

import android.content.ComponentName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspaceActionHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BasicTemplateData
import java.util.LinkedList

/**
 *  Split Target Merger, as [TargetMerger], but:
 *  - Splits out the first action into its own [SmartspaceTarget.FEATURE_WEATHER] target
 *  - Targets that are using the [SmartspaceTarget.FEATURE_WEATHER] type and are not the header are
 *  converted to [SmartspaceTarget.FEATURE_UNDEFINED] with no title.
 */
object TargetMergerSplit: TargetMerger() {

    override val blankFeatureType = SmartspaceTarget.FEATURE_UNDEFINED

    override fun getSplitTargets(
        actions: LinkedList<SmartspaceActionHolder>
    ): List<SmartspacePageHolder> {
        if(actions.isEmpty()) return emptyList()
        val action = actions.pop().let {
            if(it.action.title.isBlank()){
                it.copy(action = it.action.copy(title = it.action.subtitle?.toString() ?: ""))
            }else it
        }
        val target = SmartspaceTarget(
            smartspaceTargetId = "split_${action.action.id}",
            componentName = ComponentName(BuildConfig.APPLICATION_ID, "split"),
            headerAction = action.action,
            baseAction = action.action,
            featureType = SmartspaceTarget.FEATURE_WEATHER,
            templateData = action.action.subItemInfo?.let {
                BasicTemplateData(primaryItem = it, subtitleItem = it)
            }
        )
        return listOf(
            SmartspacePageHolder(target, null, listOf(action.parent))
        )
    }

    /**
     *  On Android 14+ with split Smartspace, there can only be one
     *  [SmartspaceTarget.FEATURE_WEATHER] target, which is clipped and used in the split space.
     *
     *  Any other incoming Targets of type [SmartspaceTarget.FEATURE_WEATHER] should be converted
     *  to [SmartspaceTarget.FEATURE_UNDEFINED], using the same method we use to create blank
     *  Targets, passing in this Target's header and base actions, as well as template data.
     */
    override fun SmartspacePageHolder.convert(): SmartspacePageHolder {
        return if(page.featureType == SmartspaceTarget.FEATURE_WEATHER){
            val page = createBlankTarget(
                page.headerAction ?: SmartspaceAction(id = "", title = ""),
                page.baseAction ?: SmartspaceAction(id = "", title = ""),
                page.templateData as? BasicTemplateData
            )
            copy(page = page)
        }else this
    }

}