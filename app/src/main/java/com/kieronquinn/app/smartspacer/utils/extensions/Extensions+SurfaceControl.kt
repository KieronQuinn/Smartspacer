package com.kieronquinn.app.smartspacer.utils.extensions

import android.view.SurfaceControlHidden
import android.view.SurfaceControl
import dev.rikka.tools.refine.Refine

fun SurfaceControl.Transaction.setBackgroundBlurRadius(
    surfaceControl: SurfaceControl, radius: Int
): SurfaceControlHidden.Transaction {
    return Refine.unsafeCast<SurfaceControlHidden.Transaction>(this)
        .setBackgroundBlurRadius(surfaceControl, radius) as SurfaceControlHidden.Transaction
}