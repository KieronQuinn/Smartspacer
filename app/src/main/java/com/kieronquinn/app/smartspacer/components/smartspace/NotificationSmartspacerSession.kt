package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig

class NotificationSmartspacerSession(
    context: Context,
    widget: AppWidget,
    config: SmartspaceConfig = widget.getConfig(),
    collectInto: suspend (AppWidget) -> Unit
) : PagedWidgetSmartspacerSession(context, widget, config, collectInto) {

    override val includeBasic = true

}