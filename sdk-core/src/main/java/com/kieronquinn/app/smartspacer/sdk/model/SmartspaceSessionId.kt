package com.kieronquinn.app.smartspacer.sdk.model

import android.os.Bundle
import android.os.Parcelable
import android.os.UserHandle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class SmartspaceSessionId(
    val id: String,
    val userHandle: UserHandle
): Parcelable {

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_USER_HANDLE = "user_handle"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getString(KEY_ID)!!,
        bundle.getParcelableCompat(KEY_USER_HANDLE, UserHandle::class.java)!!
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle(): Bundle {
        return bundleOf(
            KEY_ID to id,
            KEY_USER_HANDLE to userHandle
        )
    }

}
