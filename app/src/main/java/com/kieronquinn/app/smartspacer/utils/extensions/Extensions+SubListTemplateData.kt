package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.smartspace.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubListTemplateData
import android.app.smartspace.uitemplatedata.SubListTemplateData as SystemSubListTemplateData

fun SubListTemplateData.toSystemSubListTemplateData(tintColour: Int): SystemSubListTemplateData {
    val from = this
    return SystemSubListTemplateData.Builder(
        subListTexts.map { it.toSystemText() }
    ).apply {
        from.subListAction?.toSystemTapAction()
            .clone(::setSubListAction)
        from.subListIcon?.toSystemIcon()
            .clone(::setSubListIcon)
        from.primaryItem?.toSystemSubItemInfo(tintColour)
            .clone(::setPrimaryItem)
        from.subtitleItem?.toSystemSubItemInfo(tintColour)
            .clone(::setSubtitleItem)
        from.subtitleSupplementalItem?.toSystemSubItemInfo(tintColour)
            .clone(::setSubtitleSupplementalItem)
        from.supplementalAlarmItem?.toSystemSubItemInfo(tintColour)
            .clone(::setSupplementalAlarmItem)
        from.supplementalLineItem?.toSystemSubItemInfo(tintColour)
            .clone(::setSupplementalLineItem)
    }.build()
}

fun SubListTemplateData.cloneWithTint(colour: Int): SubListTemplateData {
    return copy(
        subListTexts = subListTexts.map { it.cloneWithTextColour(colour) },
        subListIcon = subListIcon?.cloneWithTint(colour),
        primaryItem = primaryItem?.cloneWithTint(colour),
        subtitleItem = subtitleItem?.cloneWithTint(colour),
        subtitleSupplementalItem = subtitleSupplementalItem?.cloneWithTint(colour),
        supplementalAlarmItem = supplementalAlarmItem?.cloneWithTint(colour),
        supplementalLineItem = supplementalLineItem?.cloneWithTint(colour)
    )
}

fun SystemSubListTemplateData.toSubListTemplateData(): SubListTemplateData {
    return SubListTemplateData(
        subListTexts = subListTexts.map { it.toText() },
        subListAction = subListAction?.toTapAction(),
        subListIcon = subListIcon?.toIcon(),
        primaryItem = primaryItem?.toSubItemInfo(),
        subtitleItem = subtitleItem?.toSubItemInfo(),
        subtitleSupplementalItem = subtitleSupplementalItem?.toSubItemInfo(),
        supplementalAlarmItem = supplementalAlarmItem?.toSubItemInfo(),
        supplementalLineItem = supplementalLineItem?.toSubItemInfo()
    )
}

fun SubListTemplateData.replaceActionsWithExpanded(targetId: String): SubListTemplateData {
    return copy(
        subListAction = subListAction?.replaceActionWithExpanded(targetId)
    )
}

private fun <T> T?.clone(to: (T) -> BaseTemplateData.Builder) {
    this?.let { to.invoke(it) }
}