package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class HeadToHeadTemplateData(
    val headToHeadAction: TapAction?,
    val headToHeadTitle: Text?,
    val headToHeadFirstCompetitorIcon: Icon?,
    val headToHeadFirstCompetitorText: Text?,
    val headToHeadSecondCompetitorIcon: Icon?,
    val headToHeadSecondCompetitorText: Text?,
    override var layoutWeight: Int = 0,
    override var primaryItem: SubItemInfo? = null,
    override var subtitleItem: SubItemInfo? = null,
    override var subtitleSupplementalItem: SubItemInfo? = null,
    override var supplementalAlarmItem: SubItemInfo? = null,
    override var supplementalLineItem: SubItemInfo? = null
): BaseTemplateData(
    5,
    layoutWeight,
    primaryItem,
    subtitleItem,
    subtitleSupplementalItem,
    supplementalAlarmItem,
    supplementalLineItem
) {

    companion object {
        private const val KEY_HEAD_TO_HEAD_ACTION = "head_to_head_action"
        private const val KEY_HEAD_TO_HEAD_TITLE = "head_to_head_title"
        private const val KEY_HEAD_TO_HEAD_FIRST_COMPETITOR_ICON = "head_to_head_first_competitor_icon"
        private const val KEY_HEAD_TO_HEAD_FIRST_COMPETITOR_TEXT = "head_to_head_first_competitor_text"
        private const val KEY_HEAD_TO_HEAD_SECOND_COMPETITOR_ICON = "head_to_head_second_competitor_icon"
        private const val KEY_HEAD_TO_HEAD_SECOND_COMPETITOR_TEXT = "head_to_head_second_competitor_text"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getBundle(KEY_HEAD_TO_HEAD_ACTION)?.let { TapAction(it) },
        bundle.getBundle(KEY_HEAD_TO_HEAD_TITLE)?.let { Text(it) },
        bundle.getBundle(KEY_HEAD_TO_HEAD_FIRST_COMPETITOR_ICON)?.let { Icon(it) },
        bundle.getBundle(KEY_HEAD_TO_HEAD_FIRST_COMPETITOR_TEXT)?.let { Text(it) },
        bundle.getBundle(KEY_HEAD_TO_HEAD_SECOND_COMPETITOR_ICON)?.let { Icon(it) },
        bundle.getBundle(KEY_HEAD_TO_HEAD_SECOND_COMPETITOR_TEXT)?.let { Text(it) },
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
            KEY_HEAD_TO_HEAD_ACTION to headToHeadAction?.toBundle(),
            KEY_HEAD_TO_HEAD_TITLE to headToHeadTitle?.toBundle(),
            KEY_HEAD_TO_HEAD_FIRST_COMPETITOR_ICON to headToHeadFirstCompetitorIcon?.toBundle(),
            KEY_HEAD_TO_HEAD_FIRST_COMPETITOR_TEXT to headToHeadFirstCompetitorText?.toBundle(),
            KEY_HEAD_TO_HEAD_SECOND_COMPETITOR_ICON to headToHeadSecondCompetitorIcon?.toBundle(),
            KEY_HEAD_TO_HEAD_SECOND_COMPETITOR_TEXT to headToHeadSecondCompetitorText?.toBundle()
        ))
        return bundle
    }

}
