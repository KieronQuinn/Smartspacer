package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemInfo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text

fun SubItemInfo.reformatBullet(remove: Boolean): SubItemInfo {
    return SubItemInfo(this).apply {
        text = text?.let { Text(it) }?.apply {
            text = text.reformatBullet(remove)
        }
    }
}

fun SubItemInfo.replaceActionsWithExpanded(targetId: String): SubItemInfo {
    return copy(
        tapAction = tapAction?.replaceActionWithExpanded(targetId)
    )
}

fun SubItemInfo.fixActionsIfNeeded(context: Context): SubItemInfo {
    return copy(
        tapAction = tapAction?.fixActionsIfNeeded(context)
    )
}