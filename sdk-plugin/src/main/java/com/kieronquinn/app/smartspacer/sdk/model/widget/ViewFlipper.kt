package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.widget.ViewFlipper

data class ViewFlipper(
    override val identifier: String?,
    val children: List<RemoteWidgetView<*>>
): RemoteWidgetViewGroup<ViewFlipper>(identifier, children)