package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.AnalogClock
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.model.widget.RemoteWidgetView.Companion.KEY_IDENTIFIER
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getSerializableCompat

@Suppress("DEPRECATION")
data class AnalogClock(
    override val identifier: String?,
    val dial: Icon,
    val dialTintList: ColorStateList?,
    val dialTintBlendMode: BlendMode?,
    val hourHand: Icon,
    val hourHandTintList: ColorStateList?,
    val hourHandBlendMode: BlendMode?,
    val minuteHand: Icon,
    val minuteHandTintList: ColorStateList?,
    val minuteHandBlendMode: BlendMode?,
    val secondHand: Icon,
    val secondHandTintList: ColorStateList?,
    val secondHandBlendMode: BlendMode?,
    val timeZone: String?
): RemoteWidgetView<AnalogClock> {

    companion object {
        private const val KEY_DIAL = "dial"
        private const val KEY_DIAL_TINT_LIST = "dial_tint_list"
        private const val KEY_DIAL_TINT_BLEND_MODE = "dial_tint_blend_mode"
        private const val KEY_HOUR_HAND = "hour_hand"
        private const val KEY_HOUR_HAND_TINT_LIST = "hour_hand_tint_list"
        private const val KEY_HOUR_HAND_BLEND_MODE = "hour_hand_blend_mode"
        private const val KEY_MINUTE_HAND = "minute_hand"
        private const val KEY_MINUTE_HAND_TINT_LIST = "minute_hand_tint_list"
        private const val KEY_MINUTE_HAND_BLEND_MODE = "minute_hand_blend_mode"
        private const val KEY_SECOND_HAND = "second_hand"
        private const val KEY_SECOND_HAND_TINT_LIST = "second_hand_tint_list"
        private const val KEY_SECOND_HAND_BLEND_MODE = "second_hand_blend_mode"
        private const val KEY_TIME_ZONE = "time_zone"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getString(KEY_IDENTIFIER),
        bundle.getParcelableCompat(KEY_DIAL, Icon::class.java)!!,
        bundle.getParcelableCompat(KEY_DIAL_TINT_LIST, ColorStateList::class.java),
        bundle.getSerializableCompat(KEY_DIAL_TINT_BLEND_MODE, BlendMode::class.java),
        bundle.getParcelableCompat(KEY_HOUR_HAND, Icon::class.java)!!,
        bundle.getParcelableCompat(KEY_HOUR_HAND_TINT_LIST, ColorStateList::class.java),
        bundle.getSerializableCompat(KEY_HOUR_HAND_BLEND_MODE, BlendMode::class.java),
        bundle.getParcelableCompat(KEY_MINUTE_HAND, Icon::class.java)!!,
        bundle.getParcelableCompat(KEY_MINUTE_HAND_TINT_LIST, ColorStateList::class.java),
        bundle.getSerializableCompat(KEY_MINUTE_HAND_BLEND_MODE, BlendMode::class.java),
        bundle.getParcelableCompat(KEY_SECOND_HAND, Icon::class.java)!!,
        bundle.getParcelableCompat(KEY_SECOND_HAND_TINT_LIST, ColorStateList::class.java),
        bundle.getSerializableCompat(KEY_SECOND_HAND_BLEND_MODE, BlendMode::class.java),
        bundle.getString(KEY_TIME_ZONE)!!
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun toBundle(bundle: Bundle): Bundle {
        super.toBundle(bundle)
        bundle.putParcelable(KEY_DIAL, dial)
        bundle.putParcelable(KEY_DIAL_TINT_LIST, dialTintList)
        bundle.putSerializable(KEY_DIAL_TINT_BLEND_MODE, dialTintBlendMode)
        bundle.putParcelable(KEY_HOUR_HAND, hourHand)
        bundle.putParcelable(KEY_HOUR_HAND_TINT_LIST, hourHandTintList)
        bundle.putSerializable(KEY_HOUR_HAND_BLEND_MODE, hourHandBlendMode)
        bundle.putParcelable(KEY_MINUTE_HAND, minuteHand)
        bundle.putParcelable(KEY_MINUTE_HAND_TINT_LIST, minuteHandTintList)
        bundle.putSerializable(KEY_MINUTE_HAND_BLEND_MODE, minuteHandBlendMode)
        bundle.putParcelable(KEY_SECOND_HAND, secondHand)
        bundle.putParcelable(KEY_SECOND_HAND_TINT_LIST, secondHandTintList)
        bundle.putSerializable(KEY_SECOND_HAND_BLEND_MODE, secondHandBlendMode)
        bundle.putString(KEY_TIME_ZONE, timeZone)
        return bundle
    }

}