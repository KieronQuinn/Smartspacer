package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY)
open class BaseTemplateData constructor(
    open val templateType: Int,
    open var layoutWeight: Int = 0,
    open var primaryItem: SubItemInfo? = null,
    open var subtitleItem: SubItemInfo? = null,
    open var subtitleSupplementalItem: SubItemInfo? = null,
    open var supplementalAlarmItem: SubItemInfo? = null,
    open var supplementalLineItem: SubItemInfo? = null
): Parcelable {

    companion object {
        internal const val KEY_TEMPLATE_TYPE = "template_type"
        internal const val KEY_LAYOUT_WEIGHT = "layout_weight"
        internal const val KEY_PRIMARY_ITEM = "primary_item"
        internal const val KEY_SUBTITLE_ITEM = "subtitle_item"
        internal const val KEY_SUBTITLE_SUPPLEMENTAL_ITEM = "subtitle_supplemental_item"
        internal const val KEY_SUPPLEMENTAL_ALARM_ITEM = "supplemental_alarm_item"
        internal const val KEY_SUPPLEMENTAL_LINE_ITEM = "supplemental_line_item"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getInt(KEY_TEMPLATE_TYPE),
        bundle.getInt(KEY_LAYOUT_WEIGHT),
        bundle.getBundle(KEY_PRIMARY_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUBTITLE_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUBTITLE_SUPPLEMENTAL_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUPPLEMENTAL_ALARM_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUPPLEMENTAL_LINE_ITEM)?.let { SubItemInfo(it) }
    )

    fun copy(
        templateType: Int = this.templateType,
        layoutWeight: Int = this.layoutWeight,
        primaryItem: SubItemInfo? = this.primaryItem,
        subtitleItem: SubItemInfo? = this.subtitleItem,
        subtitleSupplementalItem: SubItemInfo? = this.subtitleSupplementalItem,
        supplementalAlarmItem: SubItemInfo? = this.supplementalAlarmItem,
        supplementalLineItem: SubItemInfo? = this.supplementalLineItem
    ): BaseTemplateData {
        return BaseTemplateData(
            templateType,
            layoutWeight,
            primaryItem,
            subtitleItem,
            subtitleSupplementalItem,
            supplementalAlarmItem,
            supplementalLineItem
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    open fun toBundle(): Bundle {
        return bundleOf(
            KEY_TEMPLATE_TYPE to templateType,
            KEY_LAYOUT_WEIGHT to layoutWeight,
            KEY_PRIMARY_ITEM to primaryItem?.toBundle(),
            KEY_SUBTITLE_ITEM to subtitleItem?.toBundle(),
            KEY_SUBTITLE_SUPPLEMENTAL_ITEM to subtitleSupplementalItem?.toBundle(),
            KEY_SUPPLEMENTAL_ALARM_ITEM to supplementalAlarmItem?.toBundle(),
            KEY_SUPPLEMENTAL_LINE_ITEM to supplementalLineItem?.toBundle()
        )
    }

    @Parcelize
    data class SubItemInfo(
        var text: Text?,
        var icon: Icon? = null,
        val tapAction: TapAction? = null,
        val loggingInfo: SubItemLoggingInfo? = null
    ): Parcelable {

        companion object {
            private const val KEY_TEXT = "text"
            private const val KEY_ICON = "icon"
            private const val KEY_TAP_ACTION = "tap_action"
            private const val KEY_LOGGING_INFO = "logging_info"
        }

        constructor(clone: SubItemInfo): this(
            clone.text, clone.icon, clone.tapAction, clone.loggingInfo
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getBundle(KEY_TEXT)?.let { Text(it) },
            bundle.getBundle(KEY_ICON)?.let { Icon(it) },
            bundle.getBundle(KEY_TAP_ACTION)?.let { TapAction(it) },
            bundle.getBundle(KEY_LOGGING_INFO)?.let { SubItemLoggingInfo(it) }
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(): Bundle {
            return bundleOf(
                KEY_TEXT to text?.toBundle(),
                KEY_ICON to icon?.toBundle(),
                KEY_TAP_ACTION to tapAction?.toBundle(),
                KEY_LOGGING_INFO to loggingInfo?.toBundle()
            )
        }

        fun clone(): SubItemInfo {
            return copy(icon = icon?.clone())
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun generateSmartspaceAction(
            parent: SmartspaceTarget,
            isHeader: Boolean
        ): SmartspaceAction {
            //Only header SmartspaceActions can take an icon in legacy, otherwise skip
            val canGenerateLegacy = icon == null || isHeader
            val idSuffix = if(isHeader){
                "header"
            }else{
                "base"
            }
            return if(canGenerateLegacy){
                SmartspaceAction(
                    id = "${parent.smartspaceTargetId}_$idSuffix",
                    title = text?.text?.toString() ?: "",
                    intent = tapAction?.intent,
                    pendingIntent = tapAction?.pendingIntent,
                    subItemInfo = this
                )
            }else{
                SmartspaceAction(
                    id = "${parent.smartspaceTargetId}_$idSuffix",
                    title = "",
                    subItemInfo = this
                )
            }
        }

        override fun equals(other: Any?): Boolean {
            if(other !is SubItemInfo) return false
            if(other.text != text) return false
            if(other.tapAction != tapAction) return false
            if(other.loggingInfo != loggingInfo) return false
            return true
        }

    }

    @Parcelize
    data class SubItemLoggingInfo(
        val featureType: Int,
        val instanceId: Int,
        val packageName: String
    ): Parcelable {

        companion object {
            private const val KEY_FEATURE_TYPE = "feature_type"
            private const val KEY_INSTANCE_ID = "instance_id"
            private const val KEY_PACKAGE_NAME = "package_name"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getInt(KEY_FEATURE_TYPE),
            bundle.getInt(KEY_INSTANCE_ID),
            bundle.getString(KEY_PACKAGE_NAME)!!
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(): Bundle {
            return bundleOf(
                KEY_FEATURE_TYPE to featureType,
                KEY_INSTANCE_ID to instanceId,
                KEY_PACKAGE_NAME to packageName
            )
        }

    }

    override fun equals(other: Any?): Boolean {
        return when(templateType) {
            SmartspaceTarget.UI_TEMPLATE_DEFAULT -> {
                if(other !is BaseTemplateData) return false
                if(other.layoutWeight != layoutWeight) return false
                if(other.primaryItem != primaryItem) return false
                if(other.subtitleItem != subtitleItem) return false
                if(other.subtitleSupplementalItem != subtitleSupplementalItem) return false
                if(other.supplementalAlarmItem != supplementalAlarmItem) return false
                if(other.supplementalLineItem != supplementalLineItem) return false
                true
            }
            SmartspaceTarget.UI_TEMPLATE_CAROUSEL -> (this as CarouselTemplateData) == other
            SmartspaceTarget.UI_TEMPLATE_COMBINED_CARDS  -> (this as CombinedCardsTemplateData) == other
            SmartspaceTarget.UI_TEMPLATE_HEAD_TO_HEAD  -> (this as HeadToHeadTemplateData) == other
            SmartspaceTarget.UI_TEMPLATE_SUB_CARD -> (this as SubCardTemplateData) == other
            SmartspaceTarget.UI_TEMPLATE_SUB_IMAGE -> (this as SubImageTemplateData) == other
            SmartspaceTarget.UI_TEMPLATE_SUB_LIST -> (this as SubListTemplateData) == other
            else -> false
        }
    }

    override fun toString(): String {
        return StringBuilder().apply {
            append("BaseTemplateData ($templateType) ")
            primaryItem?.let {
                append(" primaryItem=($primaryItem) ")
            }
            subtitleItem?.let {
                append(" primaryItem=($subtitleItem) ")
            }
            subtitleSupplementalItem?.let {
                append(" subtitleSupplementalItem=($subtitleSupplementalItem) ")
            }
            supplementalAlarmItem?.let {
                append(" supplementalAlarmItem=($supplementalAlarmItem) ")
            }
            supplementalLineItem?.let {
                append(" supplementalLineItem=($supplementalLineItem) ")
            }
        }.trim().toString()
    }

    override fun hashCode(): Int {
        var result = templateType
        result = 31 * result + layoutWeight
        result = 31 * result + (primaryItem?.hashCode() ?: 0)
        result = 31 * result + (subtitleItem?.hashCode() ?: 0)
        result = 31 * result + (subtitleSupplementalItem?.hashCode() ?: 0)
        result = 31 * result + (supplementalAlarmItem?.hashCode() ?: 0)
        result = 31 * result + (supplementalLineItem?.hashCode() ?: 0)
        return result
    }

}
