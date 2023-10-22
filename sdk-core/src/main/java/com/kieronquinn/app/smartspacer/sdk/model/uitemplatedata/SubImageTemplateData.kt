package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubImageTemplateData(
    val subImages: List<Icon>,
    val subImageTexts: List<Text>,
    val subImageAction: TapAction?,
    override var layoutWeight: Int = 0,
    override var primaryItem: SubItemInfo? = null,
    override var subtitleItem: SubItemInfo? = null,
    override var subtitleSupplementalItem: SubItemInfo? = null,
    override var supplementalAlarmItem: SubItemInfo? = null,
    override var supplementalLineItem: SubItemInfo? = null
): BaseTemplateData(
    2,
    layoutWeight,
    primaryItem,
    subtitleItem,
    subtitleSupplementalItem,
    supplementalAlarmItem,
    supplementalLineItem
) {

    companion object {
        private const val KEY_SUB_IMAGES = "sub_images"
        private const val KEY_SUB_IMAGE_TEXT = "sub_image_text"
        private const val KEY_SUB_IMAGE_ACTION = "sub_image_action"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getParcelableArrayListCompat<Bundle>(KEY_SUB_IMAGES, Bundle::class.java)!!
            .map { Icon(it) },
        bundle.getParcelableArrayListCompat<Bundle>(KEY_SUB_IMAGE_TEXT, Bundle::class.java)!!
            .map { Text(it) },
        bundle.getBundle(KEY_SUB_IMAGE_ACTION)?.let { TapAction(it) },
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
            KEY_SUB_IMAGES to ArrayList(subImages.map { it.toBundle() }),
            KEY_SUB_IMAGE_TEXT to ArrayList(subImageTexts.map { it.toBundle() }),
            KEY_SUB_IMAGE_ACTION to subImageAction?.toBundle()
        ))
        return bundle
    }

}