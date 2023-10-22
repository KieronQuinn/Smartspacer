package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.widget.GridLayout

data class GridLayout(
    override val identifier: String?,
    val children: List<RemoteWidgetView<*>>
): RemoteWidgetViewGroup<GridLayout>(identifier, children)