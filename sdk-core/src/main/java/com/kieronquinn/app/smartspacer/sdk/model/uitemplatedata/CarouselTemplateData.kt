package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class CarouselTemplateData(
    val carouselItems: List<CarouselItem>,
    val carouselAction: TapAction?,
    override var layoutWeight: Int = 0,
    override var primaryItem: SubItemInfo? = null,
    override var subtitleItem: SubItemInfo? = null,
    override var subtitleSupplementalItem: SubItemInfo? = null,
    override var supplementalAlarmItem: SubItemInfo? = null,
    override var supplementalLineItem: SubItemInfo? = null
): BaseTemplateData(
    4,
    layoutWeight,
    primaryItem,
    subtitleItem,
    subtitleSupplementalItem,
    supplementalAlarmItem,
    supplementalLineItem
) {

    companion object {
        private const val KEY_CAROUSEL_ITEMS = "carousel_items"
        private const val KEY_CAROUSEL_ACTION = "carousel_action"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getParcelableArrayListCompat<Bundle>(KEY_CAROUSEL_ITEMS, Bundle::class.java)!!
            .map { CarouselItem(it) },
        bundle.getBundle(KEY_CAROUSEL_ACTION)?.let { TapAction(it) },
        bundle.getInt(KEY_LAYOUT_WEIGHT),
        bundle.getBundle(KEY_PRIMARY_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUBTITLE_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUBTITLE_SUPPLEMENTAL_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUPPLEMENTAL_ALARM_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUPPLEMENTAL_LINE_ITEM)?.let { SubItemInfo(it) }
    )

    override fun toBundle(): Bundle {
        val bundle = super.toBundle()
        bundle.putAll(bundleOf(
            KEY_CAROUSEL_ITEMS to ArrayList(carouselItems.map { it.toBundle() }),
            KEY_CAROUSEL_ACTION to carouselAction?.toBundle()
        ))
        return bundle
    }

    @Parcelize
    data class CarouselItem(
        val upperText: Text?,
        val lowerText: Text?,
        val image: Icon?,
        val tapAction: TapAction?
    ): Parcelable {

        companion object {
            private const val KEY_UPPER_TEXT = "upper_text"
            private const val KEY_LOWER_TEXT = "lower_text"
            private const val KEY_IMAGE = "image"
            private const val KEY_TAP_ACTION = "tap_action"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getBundle(KEY_UPPER_TEXT)?.let { Text(it) },
            bundle.getBundle(KEY_LOWER_TEXT)?.let { Text(it) },
            bundle.getBundle(KEY_IMAGE)?.let { Icon(it) },
            bundle.getBundle(KEY_TAP_ACTION)?.let { TapAction(it) }
        )

        fun toBundle(): Bundle {
            return bundleOf(
                KEY_UPPER_TEXT to upperText?.toBundle(),
                KEY_LOWER_TEXT to lowerText?.toBundle(),
                KEY_IMAGE to image?.toBundle(),
                KEY_TAP_ACTION to tapAction?.toBundle()
            )
        }

    }

}