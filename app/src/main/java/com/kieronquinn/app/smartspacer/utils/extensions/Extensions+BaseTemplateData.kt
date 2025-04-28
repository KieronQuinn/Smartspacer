package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemInfo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CombinedCardsTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.HeadToHeadTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubCardTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubListTemplateData

fun SubItemInfo.cloneWithTint(colour: Int): SubItemInfo {
    return copy(
        icon = icon?.cloneWithTint(colour)
    )
}

fun BaseTemplateData.clone(): BaseTemplateData {
    return when(this){
        is CarouselTemplateData -> {
            CarouselTemplateData(
                carouselItems,
                carouselAction,
                layoutWeight,
                primaryItem,
                subtitleItem,
                subtitleSupplementalItem,
                supplementalAlarmItem,
                supplementalLineItem
            )
        }
        is CombinedCardsTemplateData -> {
            CombinedCardsTemplateData(
                combinedCardDataList,
                layoutWeight,
                primaryItem,
                subtitleItem,
                subtitleSupplementalItem,
                supplementalAlarmItem,
                supplementalLineItem
            )
        }
        is HeadToHeadTemplateData -> {
            HeadToHeadTemplateData(
                headToHeadAction,
                headToHeadTitle,
                headToHeadFirstCompetitorIcon,
                headToHeadFirstCompetitorText,
                headToHeadSecondCompetitorIcon,
                headToHeadSecondCompetitorText,
                layoutWeight,
                primaryItem,
                subtitleItem,
                subtitleSupplementalItem,
                supplementalAlarmItem,
                supplementalLineItem
            )
        }
        is SubCardTemplateData -> {
            SubCardTemplateData(
                subCardText,
                subCardIcon,
                subCardAction,
                layoutWeight,
                primaryItem,
                subtitleItem,
                subtitleSupplementalItem,
                supplementalAlarmItem,
                supplementalLineItem
            )
        }
        is SubListTemplateData -> {
            SubListTemplateData(
                subListTexts,
                subListIcon,
                subListAction,
                layoutWeight,
                primaryItem,
                subtitleItem,
                subtitleSupplementalItem,
                supplementalAlarmItem,
                supplementalLineItem
            )
        }
        is SubImageTemplateData -> {
            SubImageTemplateData(
                subImages,
                subImageTexts,
                subImageAction,
                layoutWeight,
                primaryItem,
                subtitleItem,
                subtitleSupplementalItem,
                supplementalAlarmItem,
                supplementalLineItem
            )
        }
        else -> {
            BaseTemplateData(
                templateType,
                layoutWeight,
                primaryItem,
                subtitleItem,
                subtitleSupplementalItem,
                supplementalAlarmItem,
                supplementalLineItem
            )
        }
    }
}

fun BaseTemplateData.replaceActionsWithExpanded(targetId: String): BaseTemplateData {
    return clone().also {
        when(it){
            is CarouselTemplateData -> {
                it.replaceActionsWithExpanded(targetId)
            }
            is CombinedCardsTemplateData -> {
                it.replaceActionsWithExpanded(targetId)
            }
            is HeadToHeadTemplateData -> {
                it.replaceActionsWithExpanded(targetId)
            }
            is SubCardTemplateData -> {
                it.replaceActionsWithExpanded(targetId)
            }
            is SubListTemplateData -> {
                it.replaceActionsWithExpanded(targetId)
            }
            is SubImageTemplateData -> {
                it.replaceActionsWithExpanded(targetId)
            }
        }
    }.also {
        it.primaryItem = primaryItem?.replaceActionsWithExpanded(targetId)
        it.subtitleItem = subtitleItem?.replaceActionsWithExpanded(targetId)
    }
}

fun BaseTemplateData.fixActionsIfNeeded(context: Context) = apply {
    primaryItem = primaryItem?.fixActionsIfNeeded(context)
    subtitleItem = subtitleItem?.fixActionsIfNeeded(context)
    subtitleSupplementalItem = subtitleSupplementalItem?.fixActionsIfNeeded(context)
    supplementalLineItem = supplementalLineItem?.fixActionsIfNeeded(context)
    supplementalAlarmItem = supplementalAlarmItem?.fixActionsIfNeeded(context)
}