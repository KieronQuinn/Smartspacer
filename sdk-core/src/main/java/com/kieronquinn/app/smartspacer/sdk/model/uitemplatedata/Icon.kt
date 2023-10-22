package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import kotlinx.parcelize.Parcelize
import android.graphics.drawable.Icon as AndroidIcon

@Parcelize
data class Icon(
    val icon: AndroidIcon,
    var contentDescription: CharSequence? = null,
    val shouldTint: Boolean = true
): Parcelable {

    companion object {
        private const val KEY_ICON = "icon"
        private const val KEY_CONTENT_DESCRIPTION = "content_description"
        private const val KEY_SHOULD_TINT = "should_tint"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getParcelableCompat<AndroidIcon>(KEY_ICON, AndroidIcon::class.java)!!,
        bundle.getCharSequence(KEY_CONTENT_DESCRIPTION),
        bundle.getBoolean(KEY_SHOULD_TINT)
    )

    fun toBundle(): Bundle {
        return bundleOf(
            KEY_ICON to icon,
            KEY_CONTENT_DESCRIPTION to contentDescription,
            KEY_SHOULD_TINT to shouldTint
        )
    }

    fun clone(): Icon {
        return copy(icon = icon.copy())
    }

    private fun AndroidIcon.copy(): AndroidIcon {
        val parcel = Parcel.obtain()
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val icon = AndroidIcon.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        return icon
    }

}
