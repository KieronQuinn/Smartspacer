package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.smartspace.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubCardTemplateData
import android.app.smartspace.uitemplatedata.SubCardTemplateData as SystemSubCardTemplateData

fun SubCardTemplateData.toSystemSubCardTemplateData(tintColour: Int): SystemSubCardTemplateData {
    val from = this
    return SystemSubCardTemplateData.Builder(subCardIcon.toSystemIcon()).apply {
        from.subCardAction?.toSystemTapAction()
            .clone(::setSubCardAction)
        from.subCardText.toSystemText()
            .clone(::setSubCardText)
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

fun SubCardTemplateData.cloneWithTint(colour: Int): SubCardTemplateData {
    return copy(
        subCardText.cloneWithTextColour(colour),
        subCardIcon.cloneWithTint(colour) ?: subCardIcon,
        primaryItem = primaryItem?.cloneWithTint(colour),
        subtitleItem = subtitleItem?.cloneWithTint(colour),
        subtitleSupplementalItem = subtitleSupplementalItem?.cloneWithTint(colour),
        supplementalAlarmItem = supplementalAlarmItem?.cloneWithTint(colour),
        supplementalLineItem = supplementalLineItem?.cloneWithTint(colour)
    )
}

fun SystemSubCardTemplateData.toSubCardTemplateData(): SubCardTemplateData {
    return SubCardTemplateData(
        subCardIcon = subCardIcon.toIcon(),
        subCardAction = subCardAction?.toTapAction(),
        subCardText = subCardText.toText(),
        primaryItem = primaryItem?.toSubItemInfo(),
        subtitleItem = subtitleItem?.toSubItemInfo(),
        subtitleSupplementalItem = subtitleSupplementalItem?.toSubItemInfo(),
        supplementalAlarmItem = supplementalAlarmItem?.toSubItemInfo(),
        supplementalLineItem = supplementalLineItem?.toSubItemInfo()
    )
}

fun SubCardTemplateData.replaceActionsWithExpanded(targetId: String): SubCardTemplateData {
    return copy(
        subCardAction = subCardAction?.replaceActionWithExpanded(targetId)
    )
}

private fun <T> T?.clone(to: (T) -> BaseTemplateData.Builder) {
    this?.let { to.invoke(it) }
}