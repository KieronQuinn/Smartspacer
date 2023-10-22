package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.smartspace.uitemplatedata.BaseTemplateData
import android.app.smartspace.uitemplatedata.Icon
import android.app.smartspace.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData.CarouselItem
import android.app.smartspace.uitemplatedata.CarouselTemplateData as SystemCarouselTemplateData
import android.app.smartspace.uitemplatedata.CarouselTemplateData.CarouselItem as SystemCarouselItem
import android.graphics.drawable.Icon as AndroidIcon

fun CarouselTemplateData.toSystemCarouselTemplateData(tintColour: Int): SystemCarouselTemplateData {
    val from = this
    return SystemCarouselTemplateData.Builder(
        carouselItems.mapNotNull { it.toSystemCarouselItem() }.assertAtLeastOneCarouselItem()
    ).apply {
        from.carouselAction?.toSystemTapAction()
            .clone(::setCarouselAction)
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

private fun List<SystemCarouselItem>.assertAtLeastOneCarouselItem(): List<SystemCarouselItem> {
    return ifEmpty {
        listOf(SystemCarouselItem.Builder()
            .setUpperText(Text.Builder(" ").build())
            .setImage(Icon.Builder(AndroidIcon.createWithResource(
                "android", android.R.drawable.ic_delete
            )).build())
            .setLowerText(Text.Builder(" ").build())
            .build()
        )
    }
}

fun CarouselTemplateData.cloneWithTint(colour: Int): CarouselTemplateData {
    return copy(
        carouselItems = carouselItems.map { it.cloneWithTint(colour) },
        primaryItem = primaryItem?.cloneWithTint(colour),
        subtitleItem = subtitleItem?.cloneWithTint(colour),
        subtitleSupplementalItem = subtitleSupplementalItem?.cloneWithTint(colour),
        supplementalAlarmItem = supplementalAlarmItem?.cloneWithTint(colour),
        supplementalLineItem = supplementalLineItem?.cloneWithTint(colour)
    )
}

fun SystemCarouselTemplateData.toCarouselTemplateData(): CarouselTemplateData {
    return CarouselTemplateData(
        carouselItems = carouselItems.map { it.toCarouselItem() },
        carouselAction = carouselAction?.toTapAction(),
        primaryItem = primaryItem?.toSubItemInfo(),
        subtitleItem = subtitleItem?.toSubItemInfo(),
        subtitleSupplementalItem = subtitleSupplementalItem?.toSubItemInfo(),
        supplementalAlarmItem = supplementalAlarmItem?.toSubItemInfo(),
        supplementalLineItem = supplementalLineItem?.toSubItemInfo()
    )
}

fun CarouselItem.toSystemCarouselItem(): SystemCarouselItem? {
    return try {
        SystemCarouselItem.Builder()
            .setImage(image?.toSystemIcon())
            .setLowerText(lowerText?.toSystemText())
            .setUpperText(upperText?.toSystemText())
            .setTapAction(tapAction?.toSystemTapAction())
            .build()
    }catch (e: Exception){
        null //Invalid item
    }
}

fun CarouselItem.cloneWithTint(colour: Int): CarouselItem {
    return copy(
        image = image?.cloneWithTint(colour),
        lowerText = lowerText?.cloneWithTextColour(colour),
        upperText = upperText?.cloneWithTextColour(colour)
    )
}

fun SystemCarouselItem.toCarouselItem(): CarouselItem {
    return CarouselItem(
        image = image?.toIcon(),
        lowerText = lowerText?.toText(),
        upperText = upperText?.toText(),
        tapAction = tapAction?.toTapAction()
    )
}

fun CarouselTemplateData.replaceActionsWithExpanded(targetId: String): CarouselTemplateData {
    return copy(
        carouselItems = carouselItems.map { it.replaceActionsWithExpanded(targetId) }
    )
}

fun CarouselItem.replaceActionsWithExpanded(targetId: String): CarouselItem {
    return copy(
        tapAction = tapAction?.replaceActionWithExpanded(targetId)
    )
}

private fun <T> T?.clone(to: (T) -> BaseTemplateData.Builder) {
    this?.let { to.invoke(it) }
}