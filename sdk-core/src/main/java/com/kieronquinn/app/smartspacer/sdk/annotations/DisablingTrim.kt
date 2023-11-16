package com.kieronquinn.app.smartspacer.sdk.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Use with care, disabling trim may cause unexpected clipping of text if it does not fit the space"
)
@Retention(AnnotationRetention.BINARY)
annotation class DisablingTrim