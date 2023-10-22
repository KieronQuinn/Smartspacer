package com.kieronquinn.app.smartspacer.utils.extensions

import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import android.app.smartspace.SmartspaceSessionId as SystemSmartspaceSessionId

fun SystemSmartspaceSessionId.toSmartspaceSessionId(): SmartspaceSessionId {
    return SmartspaceSessionId(
        id = id!!,
        userHandle = userHandle
    )
}

fun SmartspaceSessionId.toSystemSmartspaceSessionId(): SystemSmartspaceSessionId {
    return SystemSmartspaceSessionId(id, userHandle)
}