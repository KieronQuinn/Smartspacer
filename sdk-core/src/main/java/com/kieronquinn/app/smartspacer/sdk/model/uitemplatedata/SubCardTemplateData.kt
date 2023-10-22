package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubCardTemplateData(
    val subCardText: Text,
    val subCardIcon: Icon,
    val subCardAction: TapAction?,
    override var layoutWeight: Int = 0,
    override var primaryItem: SubItemInfo? = null,
    override var subtitleItem: SubItemInfo? = null,
    override var subtitleSupplementalItem: SubItemInfo? = null,
    override var supplementalAlarmItem: SubItemInfo? = null,
    override var supplementalLineItem: SubItemInfo? = null
): BaseTemplateData(
    7,
    layoutWeight,
    primaryItem,
    subtitleItem,
    subtitleSupplementalItem,
    supplementalAlarmItem,
    supplementalLineItem
) {

    companion object {
        private const val KEY_SUB_CARD_TEXT = "sub_card_text"
        private const val KEY_SUB_CARD_ICON = "sub_card_icon"
        private const val KEY_SUB_CARD_ACTION = "sub_card_action"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        Text(bundle.getBundle(KEY_SUB_CARD_TEXT)!!),
        Icon(bundle.getBundle(KEY_SUB_CARD_ICON)!!),
        bundle.getBundle(KEY_SUB_CARD_ACTION)?.let { TapAction(it) },
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
            KEY_SUB_CARD_TEXT to subCardText.toBundle(),
            KEY_SUB_CARD_ICON to subCardIcon.toBundle(),
            KEY_SUB_CARD_ACTION to subCardAction?.toBundle()
        ))
        return bundle
    }

}