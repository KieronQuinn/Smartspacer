package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.smartspace.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.HeadToHeadTemplateData
import android.app.smartspace.uitemplatedata.HeadToHeadTemplateData as SystemHeadToHeadTemplateData

fun HeadToHeadTemplateData.toSystemHeadToHeadTemplateData(tintColour: Int): SystemHeadToHeadTemplateData {
    val from = this
    return SystemHeadToHeadTemplateData.Builder().apply {
        from.headToHeadAction?.toSystemTapAction()
            .clone(::setHeadToHeadAction)
        from.headToHeadFirstCompetitorIcon?.toSystemIcon()
            .clone(::setHeadToHeadFirstCompetitorIcon)
        from.headToHeadFirstCompetitorText?.toSystemText()
            .clone(::setHeadToHeadFirstCompetitorText)
        from.headToHeadSecondCompetitorIcon?.toSystemIcon()
            .clone(::setHeadToHeadSecondCompetitorIcon)
        from.headToHeadSecondCompetitorText?.toSystemText()
            .clone(::setHeadToHeadSecondCompetitorText)
        from.headToHeadTitle?.toSystemText()
            .clone(::setHeadToHeadTitle)
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

fun HeadToHeadTemplateData.cloneWithTint(colour: Int): HeadToHeadTemplateData {
    return copy(
        headToHeadFirstCompetitorIcon = headToHeadFirstCompetitorIcon?.cloneWithTint(colour),
        headToHeadFirstCompetitorText = headToHeadFirstCompetitorText?.cloneWithTextColour(colour),
        headToHeadSecondCompetitorIcon = headToHeadSecondCompetitorIcon?.cloneWithTint(colour),
        headToHeadSecondCompetitorText = headToHeadSecondCompetitorText?.cloneWithTextColour(colour),
        headToHeadTitle = headToHeadTitle?.cloneWithTextColour(colour),
        primaryItem = primaryItem?.cloneWithTint(colour),
        subtitleItem = subtitleItem?.cloneWithTint(colour),
        subtitleSupplementalItem = subtitleSupplementalItem?.cloneWithTint(colour),
        supplementalAlarmItem = supplementalAlarmItem?.cloneWithTint(colour),
        supplementalLineItem = supplementalLineItem?.cloneWithTint(colour)
    )
}

fun HeadToHeadTemplateData.replaceActionsWithExpanded(targetId: String): HeadToHeadTemplateData {
    return copy(
        headToHeadAction = headToHeadAction?.replaceActionWithExpanded(targetId)
    )
}

fun SystemHeadToHeadTemplateData.toHeadToHeadTemplateData(): HeadToHeadTemplateData {
    return HeadToHeadTemplateData(
        headToHeadAction = headToHeadAction?.toTapAction(),
        headToHeadFirstCompetitorIcon = headToHeadFirstCompetitorIcon?.toIcon(),
        headToHeadFirstCompetitorText = headToHeadFirstCompetitorText?.toText(),
        headToHeadSecondCompetitorIcon = headToHeadSecondCompetitorIcon?.toIcon(),
        headToHeadSecondCompetitorText = headToHeadSecondCompetitorText?.toText(),
        headToHeadTitle = headToHeadTitle?.toText(),
        primaryItem = primaryItem?.toSubItemInfo(),
        subtitleItem = subtitleItem?.toSubItemInfo(),
        subtitleSupplementalItem = subtitleSupplementalItem?.toSubItemInfo(),
        supplementalAlarmItem = supplementalAlarmItem?.toSubItemInfo(),
        supplementalLineItem = supplementalLineItem?.toSubItemInfo()
    )
}

private fun <T> T?.clone(to: (T) -> BaseTemplateData.Builder) {
    this?.let { to.invoke(it) }
}