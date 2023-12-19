package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig

class NotificationSmartspacerSession(
    context: Context,
    widget: AppWidget,
    config: SmartspaceConfig = widget.getConfig(),
    override val includeBasic: Boolean,
    collectInto: suspend (AppWidget) -> Unit
) : WidgetSmartspacerSession(context, widget, config, collectInto)