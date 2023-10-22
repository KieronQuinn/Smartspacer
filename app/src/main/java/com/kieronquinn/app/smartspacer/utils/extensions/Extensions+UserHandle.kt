package com.kieronquinn.app.smartspacer.utils.extensions

import android.os.UserHandle
import android.os.UserHandleHidden
import dev.rikka.tools.refine.Refine

fun UserHandle.getIdentifier(): Int {
    return Refine.unsafeCast<UserHandleHidden>(this).identifier
}