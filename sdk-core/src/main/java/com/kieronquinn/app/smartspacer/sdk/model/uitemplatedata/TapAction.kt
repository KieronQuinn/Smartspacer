package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.os.Process
import android.os.UserHandle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class TapAction(
    val id: CharSequence = UUID.randomUUID().toString(),
    val intent: Intent? = null,
    val pendingIntent: PendingIntent? = null,
    val extras: Bundle = Bundle.EMPTY,
    var shouldShowOnLockScreen: Boolean = false,
    val userHandle: UserHandle = Process.myUserHandle()
): Parcelable {

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_INTENT = "intent"
        private const val KEY_PENDING_INTENT = "pending_intent"
        private const val KEY_EXTRAS = "extras"
        private const val KEY_SHOULD_SHOW_ON_LOCK_SCREEN = "should_show_on_lock_screen"
        private const val KEY_USER_HANDLE = "user_handle"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getCharSequence(KEY_ID)!!,
        bundle.getParcelableCompat(KEY_INTENT, Intent::class.java),
        bundle.getParcelableCompat(KEY_PENDING_INTENT, PendingIntent::class.java),
        bundle.getParcelableCompat(KEY_EXTRAS, Bundle::class.java)!!,
        bundle.getBoolean(KEY_SHOULD_SHOW_ON_LOCK_SCREEN),
        bundle.getParcelableCompat(KEY_USER_HANDLE, UserHandle::class.java)!!
    )

    fun toBundle(): Bundle {
        return bundleOf(
            KEY_ID to id,
            KEY_INTENT to intent,
            KEY_PENDING_INTENT to pendingIntent,
            KEY_EXTRAS to extras,
            KEY_SHOULD_SHOW_ON_LOCK_SCREEN to shouldShowOnLockScreen,
            KEY_USER_HANDLE to userHandle
        )
    }

    override fun equals(other: Any?): Boolean {
        if(other !is TapAction) return false
        //Matches system - intent data is not static so should not be checked
        return other.id == id
    }

}