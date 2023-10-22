package com.kieronquinn.app.smartspacer.sdk.model

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf

sealed class CompatibilityState {
    object Compatible: CompatibilityState()
    data class Incompatible(val reason: CharSequence?): CompatibilityState()

    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_INCOMPATIBLE_REASON = "reason"
        private const val TYPE_COMPATIBLE = 0
        private const val TYPE_INCOMPATIBLE = 1

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): CompatibilityState {
            return when(bundle.getInt(KEY_TYPE)){
                TYPE_COMPATIBLE -> Compatible
                TYPE_INCOMPATIBLE -> {
                    Incompatible(
                        bundle.getCharSequence(
                            KEY_INCOMPATIBLE_REASON
                        )
                    )
                }
                else -> Incompatible(
                    null
                )
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle(): Bundle {
        val type = when(this){
            is Compatible -> TYPE_COMPATIBLE
            is Incompatible -> TYPE_INCOMPATIBLE
        }
        return bundleOf(
            KEY_TYPE to type
        ).also {
            if(this is Incompatible){
                it.putCharSequence(KEY_INCOMPATIBLE_REASON, reason)
            }
        }
    }
}