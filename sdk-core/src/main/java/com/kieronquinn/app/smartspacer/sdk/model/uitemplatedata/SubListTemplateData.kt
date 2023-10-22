package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubListTemplateData(
    val subListTexts: List<Text>,
    val subListIcon: Icon?,
    val subListAction: TapAction?,
    override var layoutWeight: Int = 0,
    override var primaryItem: SubItemInfo? = null,
    override var subtitleItem: SubItemInfo? = null,
    override var subtitleSupplementalItem: SubItemInfo? = null,
    override var supplementalAlarmItem: SubItemInfo? = null,
    override var supplementalLineItem: SubItemInfo? = null
): BaseTemplateData(
    3,
    layoutWeight,
    primaryItem,
    subtitleItem,
    subtitleSupplementalItem,
    supplementalAlarmItem,
    supplementalLineItem
) {

    companion object {
        private const val KEY_SUB_LIST_TEXTS = "sub_list_texts"
        private const val KEY_SUB_LIST_ICON = "sub_list_icon"
        private const val KEY_SUB_LIST_ACTION = "sub_list_action"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getParcelableArrayListCompat<Bundle>(KEY_SUB_LIST_TEXTS, Bundle::class.java)!!
            .map { Text(it) },
        bundle.getBundle(KEY_SUB_LIST_ICON)?.let { Icon(it) },
        bundle.getBundle(KEY_SUB_LIST_ACTION)?.let { TapAction(it) },
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
            KEY_SUB_LIST_TEXTS to ArrayList(subListTexts.map { it.toBundle() }),
            KEY_SUB_LIST_ICON to subListIcon?.toBundle(),
            KEY_SUB_LIST_ACTION to subListAction?.toBundle()
        ))
        return bundle
    }

}
