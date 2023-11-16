package com.kieronquinn.app.smartspacer.sdk.utils

import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.annotations.DisablingTrim
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemInfo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import java.util.UUID

sealed class ComplicationTemplate {

    companion object {
        private const val SUBTITLE_MAX_LENGTH = 12
        private const val EXTRA_SUBCARD_TYPE = "subcardType"
        private const val EXTRA_DISMISS_INTENT = "dismiss_intent"
        private const val SUBCARD_TYPE_WEATHER = SmartspaceTarget.FEATURE_WEATHER

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun setSubcardTypeToWeather(bundle: Bundle) {
            bundle.putInt(EXTRA_SUBCARD_TYPE, SUBCARD_TYPE_WEATHER)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun shouldTint(smartspaceAction: SmartspaceAction?): Boolean {
            return smartspaceAction?.extras?.getInt(
                EXTRA_SUBCARD_TYPE, SmartspaceTarget.FEATURE_UNDEFINED
            ) != SUBCARD_TYPE_WEATHER
        }

        fun blank(): ComplicationTemplate {
            return Basic(
                UUID.randomUUID().toString(),
                icon = null,
                content = Text(" "),
                onClick = null
            )
        }

    }

    /**
     *  Basic complication showing an icon and some text.
     *
     *  The [content] text will be truncated to [SUBTITLE_MAX_LENGTH] characters, in order to fit
     *  in the Native Smartspace, which does not play nice with longer text.
     */
    data class Basic(
        val id: String,
        val icon: Icon? = null,
        val content: Text? = null,
        val onClick: TapAction?,
        val extras: Bundle = Bundle.EMPTY,
        val trimToFit: TrimToFit = TrimToFit.Enabled
    ): ComplicationTemplate() {

        override fun create(): SmartspaceAction {
            content?.text?.let {
                if(trimToFit is TrimToFit.Enabled) {
                    //Trim the content to max 6 chars to fit in Native Smartspace
                    content.text = it.takeEllipsised(SUBTITLE_MAX_LENGTH)
                }
            }
            //If the icon isn't tinted, also include the subcardType extra to disable legacy tinting
            val extrasCompat = Bundle().apply {
                putAll(extras)
                if(icon != null && !icon.shouldTint){
                    setSubcardTypeToWeather(this)
                    //Needed to shut media smartspace up on 14 B1+
                    putParcelable(EXTRA_DISMISS_INTENT, Intent())
                }
            }
            return SmartspaceAction(
                id = id,
                icon = icon?.icon,
                title = "",
                subtitle = content?.text,
                intent = onClick?.intent,
                pendingIntent = onClick?.pendingIntent,
                extras = extrasCompat,
                subItemInfo = SubItemInfo(
                    text = content,
                    icon = icon,
                    tapAction = onClick
                )
            )
        }

    }

    abstract fun create(): SmartspaceAction

}

sealed class TrimToFit {
    data object Enabled: TrimToFit()
    @DisablingTrim
    data object Disabled: TrimToFit()
}