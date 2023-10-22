package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget

data class BasicTemplateData(
    override var layoutWeight: Int = 0,
    override var primaryItem: SubItemInfo? = null,
    override var subtitleItem: SubItemInfo? = null,
    override var subtitleSupplementalItem: SubItemInfo? = null,
    override var supplementalAlarmItem: SubItemInfo? = null,
    override var supplementalLineItem: SubItemInfo? = null
): BaseTemplateData(
    SmartspaceTarget.UI_TEMPLATE_DEFAULT,
    layoutWeight,
    primaryItem,
    subtitleItem,
    subtitleSupplementalItem,
    supplementalAlarmItem,
    supplementalLineItem
) {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getInt(KEY_LAYOUT_WEIGHT),
        bundle.getBundle(KEY_PRIMARY_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUBTITLE_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUBTITLE_SUPPLEMENTAL_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUPPLEMENTAL_ALARM_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUPPLEMENTAL_LINE_ITEM)?.let { SubItemInfo(it) }
    )

    final override fun toBundle(): Bundle {
        return super.toBundle()
    }

}