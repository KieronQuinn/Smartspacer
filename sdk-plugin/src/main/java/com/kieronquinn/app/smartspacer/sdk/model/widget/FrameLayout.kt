package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.widget.FrameLayout

data class FrameLayout(
    override val identifier: String?,
    val children: List<RemoteWidgetView<*>>
): RemoteWidgetViewGroup<FrameLayout>(identifier, children)