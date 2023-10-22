package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils.TruncateAt
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class Text(
    var text: CharSequence,
    val truncateAtType: TruncateAt = TruncateAt.END,
    val maxLines: Int = 1
): Parcelable {

    companion object {
        private const val KEY_TEXT = "text"
        private const val KEY_TRUNCATE_AT_TYPE = "truncate_at_type"
        private const val KEY_MAX_LINES = "max_lines"
    }

    constructor(clone: Text): this(
        clone.text,
        clone.truncateAtType,
        clone.maxLines
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getCharSequence(KEY_TEXT)!!,
        bundle.getSerializable(KEY_TRUNCATE_AT_TYPE) as TruncateAt,
        bundle.getInt(KEY_MAX_LINES)
    )

    fun toBundle(): Bundle {
        return bundleOf(
            KEY_TEXT to text,
            KEY_TRUNCATE_AT_TYPE to truncateAtType,
            KEY_MAX_LINES to maxLines
        )
    }

}
