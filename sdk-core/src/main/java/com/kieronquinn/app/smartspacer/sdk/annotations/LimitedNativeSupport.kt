package com.kieronquinn.app.smartspacer.sdk.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This field is only supported by certain Smartspace implementations, check the documentation for more info"
)
@Retention(AnnotationRetention.BINARY)
annotation class LimitedNativeSupport