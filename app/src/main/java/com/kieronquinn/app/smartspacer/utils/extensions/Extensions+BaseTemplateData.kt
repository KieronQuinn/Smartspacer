package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.*
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemInfo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemLoggingInfo
import android.app.smartspace.uitemplatedata.BaseTemplateData as SystemBaseTemplateData
import android.app.smartspace.uitemplatedata.BaseTemplateData.SubItemInfo as SystemSubItemInfo
import android.app.smartspace.uitemplatedata.BaseTemplateData.SubItemLoggingInfo as SystemSubItemLoggingInfo
import android.app.smartspace.uitemplatedata.CarouselTemplateData as SystemCarouselTemplateData
import android.app.smartspace.uitemplatedata.CombinedCardsTemplateData as SystemCombinedCardsTemplateData
import android.app.smartspace.uitemplatedata.HeadToHeadTemplateData as SystemHeadToHeadTemplateData
import android.app.smartspace.uitemplatedata.SubCardTemplateData as SystemSubCardTemplateData
import android.app.smartspace.uitemplatedata.SubImageTemplateData as SystemSubImageTemplateData
import android.app.smartspace.uitemplatedata.SubListTemplateData as SystemSubListTemplateData
import android.app.smartspace.uitemplatedata.Text as SystemText

fun BaseTemplateData.toSystemBaseTemplateData(
    tintColour: Int
): SystemBaseTemplateData {
    return when(this){
        is CarouselTemplateData -> {
            cloneWithTint(tintColour).toSystemCarouselTemplateData(tintColour)
        }
        is CombinedCardsTemplateData -> {
            toSystemCombinedCardsTemplateData(tintColour)
        }
        is HeadToHeadTemplateData -> {
            cloneWithTint(tintColour).toSystemHeadToHeadTemplateData(tintColour)
        }
        is SubCardTemplateData -> {
            cloneWithTint(tintColour).toSystemSubCardTemplateData(tintColour)
        }
        is SubListTemplateData -> {
            cloneWithTint(tintColour).toSystemSubListTemplateData(tintColour)
        }
        is SubImageTemplateData -> {
            cloneWithTint(tintColour).toSystemSubImageTemplateData(tintColour)
        }
        else -> {
            val from = copy(
                primaryItem = primaryItem?.cloneWithTint(tintColour),
                subtitleItem = subtitleItem?.cloneWithTint(tintColour),
                subtitleSupplementalItem = subtitleSupplementalItem?.cloneWithTint(tintColour),
                supplementalAlarmItem = supplementalAlarmItem?.cloneWithTint(tintColour),
                supplementalLineItem = supplementalLineItem?.cloneWithTint(tintColour)
            )
            SystemBaseTemplateData.Builder(templateType).apply {
                from.layoutWeight.clone(::setLayoutWeight)
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
    }
}

fun SystemBaseTemplateData.toBaseTemplateData(): BaseTemplateData {
    return when(this){
        is SystemCarouselTemplateData -> toCarouselTemplateData()
        is SystemCombinedCardsTemplateData -> toCombinedCardsTemplateData()
        is SystemHeadToHeadTemplateData -> toHeadToHeadTemplateData()
        is SystemSubCardTemplateData -> toSubCardTemplateData()
        is SystemSubListTemplateData -> toSubListTemplateData()
        is SystemSubImageTemplateData -> toSubImageTemplateData()
        else -> BaseTemplateData(
            templateType = templateType,
            layoutWeight = layoutWeight,
            primaryItem = primaryItem?.toSubItemInfo(),
            subtitleItem = subtitleItem?.toSubItemInfo(),
            subtitleSupplementalItem = subtitleSupplementalItem?.toSubItemInfo(),
            supplementalAlarmItem = supplementalAlarmItem?.toSubItemInfo(),
            supplementalLineItem = supplementalLineItem?.toSubItemInfo()
        )
    }
}

fun SubItemInfo.toSystemSubItemInfo(tintColour: Int): SystemSubItemInfo {
    val tinted = cloneWithTint(tintColour)
    return SystemSubItemInfo.Builder()
        .setText(tinted.text?.toSystemText()?.takeIf { it.text.isNotEmpty() }
            ?: SystemText.Builder(" ").build())
        .setIcon(tinted.icon?.toSystemIcon())
        .setTapAction(tinted.tapAction?.toSystemTapAction())
        .setLoggingInfo(tinted.loggingInfo?.toSystemSubItemLoggingInfo())
        .build()
}

fun SubItemInfo.cloneWithTint(colour: Int): SubItemInfo {
    return copy(
        text = text?.cloneWithTextColour(colour),
        icon = icon?.cloneWithTint(colour)
    )
}

fun SystemSubItemInfo.toSubItemInfo(): SubItemInfo {
    return SubItemInfo(
        text = text?.toText(),
        icon = icon?.toIcon(),
        tapAction = tapAction?.toTapAction(),
        loggingInfo = loggingInfo?.toSubItemLoggingInfo()
    )
}

fun SubItemLoggingInfo.toSystemSubItemLoggingInfo(): SystemSubItemLoggingInfo {
    return SystemSubItemLoggingInfo.Builder(instanceId, featureType)
        .setPackageName(packageName)
        .build()
}

fun SystemSubItemLoggingInfo.toSubItemLoggingInfo(): SubItemLoggingInfo {
    return SubItemLoggingInfo(
        instanceId = instanceId,
        featureType = featureType,
        packageName = packageName.toString()
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

private fun <T> T?.clone(to: (T) -> SystemBaseTemplateData.Builder) {
    this?.let { to.invoke(it) }
}