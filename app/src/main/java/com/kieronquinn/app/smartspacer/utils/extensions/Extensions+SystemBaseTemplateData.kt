package com.kieronquinn.app.smartspacer.utils.extensions

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

private fun <T> T?.clone(to: (T) -> SystemBaseTemplateData.Builder) {
    this?.let { to.invoke(it) }
}