package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.widget.RelativeLayout

data class RelativeLayout(
    override val identifier: String?,
    val children: List<RemoteWidgetView<*>>
): RemoteWidgetViewGroup<RelativeLayout>(identifier, children)