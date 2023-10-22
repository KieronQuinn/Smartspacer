package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.smartspace.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import android.app.smartspace.uitemplatedata.SubImageTemplateData as SystemSubImageTemplateData

fun SubImageTemplateData.toSystemSubImageTemplateData(tintColour: Int): SystemSubImageTemplateData {
    val from = this
    return SystemSubImageTemplateData.Builder(
        subImageTexts.map { it.toSystemText() },
        subImages.map { it.toSystemIcon() }
    ).setSubImageAction(subImageAction?.toSystemTapAction()).apply {
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

fun SystemSubImageTemplateData.toSubImageTemplateData(): SubImageTemplateData {
    return SubImageTemplateData(
        subImages = subImages.map { it.toIcon() },
        subImageTexts = subImageTexts.map { it.toText() },
        subImageAction = subImageAction?.toTapAction(),
        primaryItem = primaryItem?.toSubItemInfo(),
        subtitleItem = subtitleItem?.toSubItemInfo(),
        subtitleSupplementalItem = subtitleSupplementalItem?.toSubItemInfo(),
        supplementalAlarmItem = supplementalAlarmItem?.toSubItemInfo(),
        supplementalLineItem = supplementalLineItem?.toSubItemInfo()
    )
}

fun SubImageTemplateData.cloneWithTint(colour: Int): SubImageTemplateData {
    return copy(
        subImages.mapNotNull { it.cloneWithTint(colour) },
        subImageTexts.map { it.cloneWithTextColour(colour) },
        primaryItem = primaryItem?.cloneWithTint(colour),
        subtitleItem = subtitleItem?.cloneWithTint(colour),
        subtitleSupplementalItem = subtitleSupplementalItem?.cloneWithTint(colour),
        supplementalAlarmItem = supplementalAlarmItem?.cloneWithTint(colour),
        supplementalLineItem = supplementalLineItem?.cloneWithTint(colour)
    )
}

fun SubImageTemplateData.replaceActionsWithExpanded(targetId: String): SubImageTemplateData {
    return copy(
        subImageAction = subImageAction?.replaceActionWithExpanded(targetId)
    )
}

private fun <T> T?.clone(to: (T) -> BaseTemplateData.Builder) {
    this?.let { to.invoke(it) }
}