package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.widget.LinearLayout

data class LinearLayout(
    override val identifier: String?,
    val children: List<RemoteWidgetView<*>>
): RemoteWidgetViewGroup<LinearLayout>(identifier, children)