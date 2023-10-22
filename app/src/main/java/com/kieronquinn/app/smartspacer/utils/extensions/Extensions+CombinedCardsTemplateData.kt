package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.smartspace.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CombinedCardsTemplateData
import android.app.smartspace.uitemplatedata.CombinedCardsTemplateData as SystemCombinedCardsTemplateData

fun CombinedCardsTemplateData.toSystemCombinedCardsTemplateData(
    tintColour: Int
): SystemCombinedCardsTemplateData {
    val from = this
    return SystemCombinedCardsTemplateData.Builder(
        combinedCardDataList.map { it.toSystemBaseTemplateData(tintColour = tintColour) }
    ).apply {
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

fun SystemCombinedCardsTemplateData.toCombinedCardsTemplateData(): CombinedCardsTemplateData {
    return CombinedCardsTemplateData(
        combinedCardDataList = combinedCardDataList.map { it.toBaseTemplateData() },
        primaryItem = primaryItem?.toSubItemInfo(),
        subtitleItem = subtitleItem?.toSubItemInfo(),
        subtitleSupplementalItem = subtitleSupplementalItem?.toSubItemInfo(),
        supplementalAlarmItem = supplementalAlarmItem?.toSubItemInfo(),
        supplementalLineItem = supplementalLineItem?.toSubItemInfo()
    )
}

fun CombinedCardsTemplateData.replaceActionsWithExpanded(targetId: String): CombinedCardsTemplateData {
    return copy(
        combinedCardDataList = combinedCardDataList.map { it.replaceActionsWithExpanded(targetId) }
    )
}

private fun <T> T?.clone(to: (T) -> BaseTemplateData.Builder) {
    this?.let { to.invoke(it) }
}