package com.kieronquinn.app.smartspacer.sdk.utils

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.annotation.RestrictTo
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.*
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemInfo
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_FRAME_DURATION_MS
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_IMAGE_BITMAP
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_IMAGE_LAYOUT_HEIGHT
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_IMAGE_LAYOUT_WIDTH
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_IMAGE_RATIO_HEIGHT
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_IMAGE_RATIO_WIDTH
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_IMAGE_SCALE_TYPE
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_IMAGE_URI
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_LOADING_ICON_HEIGHT
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_LOADING_ICON_WIDTH
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_LOADING_SCREEN_ICON
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_LOADING_SCREEN_STATE
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_PROGRESS_BAR_HEIGHT
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_PROGRESS_BAR_VISIBLE
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_PROGRESS_BAR_WIDTH
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Doorbell.Companion.KEY_TINT_LOADING_ICON
import android.graphics.drawable.Icon as AndroidIcon

sealed class TargetTemplate {

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        val FEATURE_DENYLIST_BASIC = arrayOf(
            SmartspaceTarget.FEATURE_COMBINATION_AT_STORE,
            SmartspaceTarget.FEATURE_COMBINATION,
            SmartspaceTarget.FEATURE_FLIGHT,
            SmartspaceTarget.FEATURE_SPORTS,
            SmartspaceTarget.FEATURE_WEATHER_ALERT,
            SmartspaceTarget.FEATURE_SHOPPING_LIST,
            SmartspaceTarget.FEATURE_LOYALTY_CARD,
            SmartspaceTarget.FEATURE_COMMUTE_TIME,
            SmartspaceTarget.FEATURE_ETA_MONITORING,
            SmartspaceTarget.FEATURE_PACKAGE_TRACKING,
            SmartspaceTarget.FEATURE_DOORBELL
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        val FEATURE_ALLOWLIST_IMAGE = arrayOf(
            SmartspaceTarget.FEATURE_COMMUTE_TIME,
            SmartspaceTarget.FEATURE_ETA_MONITORING
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        val FEATURE_ALLOWLIST_DOORBELL = arrayOf(
            SmartspaceTarget.FEATURE_PACKAGE_TRACKING,
            SmartspaceTarget.FEATURE_DOORBELL
        )

        private fun Bitmap_createEmptyBitmap(): Bitmap {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        }
    }

    /**
     *  Basic, generic full size target. Shows a title, subtitle and icon with no extra content.
     *
     *  Accepts all [featureType] values, except those listed in [FEATURE_DENYLIST_BASIC].
     */
    data class Basic(
        val id: String,
        val componentName: ComponentName,
        val featureType: Int = SmartspaceTarget.FEATURE_UNDEFINED,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val onClick: TapAction? = null,
        val subComplication: SmartspaceAction? = null
    ): TargetTemplate() {
        override fun create(): SmartspaceTarget {
            if(FEATURE_DENYLIST_BASIC.contains(featureType)){
                throw InvalidTemplateException(
                    "Feature type $featureType is invalid for Basic template"
                )
            }
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    icon = icon?.icon,
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                //A blank action will be replaced by the merger, but allows undocumented extras
                baseAction = subComplication ?: createBlankAction(),
                featureType = featureType,
                componentName = componentName,
                templateData = BasicTemplateData(
                    primaryItem = SubItemInfo(
                        text = title,
                        tapAction = onClick
                    ),
                    subtitleItem = subtitle?.let {
                        SubItemInfo(
                            text = subtitle,
                            icon = icon,
                            tapAction = onClick
                        )
                    },
                    subtitleSupplementalItem = subComplication?.subItemInfo
                )
            )
        }
    }

    /**
     *  Full size target, shows a title, icon and subtitle, and in addition a small summary
     *  (eg. a time), and two competitor scores with icons
     */
    data class HeadToHead(
        val context: Context,
        val id: String,
        val componentName: ComponentName,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val onClick: TapAction?,
        val headToHeadTitle: Text,
        val headToHeadFirstCompetitorIcon: Icon,
        val headToHeadFirstCompetitorText: Text,
        val headToHeadSecondCompetitorIcon: Icon,
        val headToHeadSecondCompetitorText: Text
    ): TargetTemplate() {

        companion object {
            const val KEY_MATCH_TIME_SUMMARY = "matchTimeSummary"
            const val KEY_FIRST_COMPETITOR_SCORE = "firstCompetitorScore"
            const val KEY_SECOND_COMPETITOR_SCORE = "secondCompetitorScore"
            const val KEY_FIRST_COMPETITOR_LOGO = "firstCompetitorLogo"
            const val KEY_SECOND_COMPETITOR_LOGO = "secondCompetitorLogo"
        }

        override fun create(): SmartspaceTarget {
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    icon = icon?.icon,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                baseAction = SmartspaceAction(
                    id = "${id}_base",
                    title = "",
                    extras = bundleOf(
                        KEY_MATCH_TIME_SUMMARY to headToHeadTitle,
                        KEY_FIRST_COMPETITOR_SCORE to headToHeadFirstCompetitorText,
                        KEY_SECOND_COMPETITOR_SCORE to headToHeadSecondCompetitorText,
                        KEY_FIRST_COMPETITOR_LOGO to headToHeadFirstCompetitorIcon.toBitmap(context),
                        KEY_SECOND_COMPETITOR_LOGO to headToHeadSecondCompetitorIcon.toBitmap(context)
                    ),
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                featureType = SmartspaceTarget.FEATURE_SPORTS,
                componentName = componentName,
                templateData = HeadToHeadTemplateData(
                    headToHeadAction = TapAction(
                        id = "${id}_base",
                        intent = onClick?.intent,
                        pendingIntent = onClick?.pendingIntent
                    ),
                    headToHeadTitle = headToHeadTitle,
                    headToHeadFirstCompetitorIcon = headToHeadFirstCompetitorIcon,
                    headToHeadFirstCompetitorText = headToHeadFirstCompetitorText,
                    headToHeadSecondCompetitorIcon = headToHeadSecondCompetitorIcon,
                    headToHeadSecondCompetitorText = headToHeadSecondCompetitorText,
                    primaryItem = SubItemInfo(
                        text = title,
                        tapAction = onClick
                    ),
                    subtitleItem = subtitle?.let {
                        SubItemInfo(
                            text = it,
                            icon = icon,
                            tapAction = onClick
                        )
                    },
                )
            )
        }
    }

    /**
     *  Full size target, showing a title, subtitle and icon, and additionally a large button-style
     *  view with a text and icon
     *
     *  Due to limited support for [SmartspaceTarget.FEATURE_COMBINATION] on Android 12, this uses
     *  [SmartspaceTarget.FEATURE_LOYALTY_CARD].
     */
    data class Button(
        val context: Context,
        val id: String,
        val componentName: ComponentName,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val onClick: TapAction?,
        val buttonIcon: Icon,
        val buttonText: Text
    ): TargetTemplate() {

        companion object {
            const val KEY_CARD_PROMPT = "cardPrompt"
            const val KEY_IMAGE_BITMAP = "imageBitmap"
        }

        override fun create(): SmartspaceTarget {
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    icon = icon?.icon,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                baseAction = SmartspaceAction(
                    id = "${id}_base",
                    title = "",
                    extras = bundleOf(KEY_CARD_PROMPT to buttonText.text.toString()).also {
                        it.putParcelable(KEY_IMAGE_BITMAP, buttonIcon.toBitmap(context))
                    },
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                featureType = SmartspaceTarget.FEATURE_LOYALTY_CARD,
                componentName = componentName,
                templateData = SubCardTemplateData(
                    subCardText = buttonText,
                    subCardIcon = buttonIcon,
                    subCardAction = onClick,
                    primaryItem = SubItemInfo(
                        text = title,
                        tapAction = onClick
                    ),
                    subtitleItem = subtitle?.let {
                        SubItemInfo(
                            text = it,
                            icon = icon,
                            tapAction = onClick
                        )
                    }
                )
            )
        }
    }

    /**
     *  Full size target, showing a title, subtitle and icon, and additionally a list with up to 3
     *  items, or an empty list message. Empty list message is unused on Android 13+  (send it as
     *  the first item instead for better support)
     */
    data class ListItems(
        val id: String,
        val componentName: ComponentName,
        val context: Context,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val listItems: List<Text>,
        val listIcon: Icon,
        val emptyListMessage: Text,
        val onClick: TapAction?
    ): TargetTemplate() {

        companion object {
            private const val LIST_ITEM_MAX_LENGTH = 25
            const val KEY_EMPTY_LIST_STRING = "emptyListString"
            const val KEY_LIST_ITEMS = "listItems"
            const val KEY_APP_ICON = "appIcon"
        }

        override fun create(): SmartspaceTarget {
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    icon = icon?.icon,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                baseAction = SmartspaceAction(
                    id = "${id}_base",
                    title = "",
                    extras = if(listItems.isEmpty()){
                        bundleOf(KEY_EMPTY_LIST_STRING to emptyListMessage.text.toString())
                    }else{
                        bundleOf(KEY_LIST_ITEMS to listItems.map { it.toString() }.toTypedArray())
                    }.also {
                        it.putParcelable(KEY_APP_ICON, listIcon.toBitmap(context))
                    },
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                featureType = SmartspaceTarget.FEATURE_SHOPPING_LIST,
                componentName = componentName,
                templateData = SubListTemplateData(
                    subListTexts = listItems.clipToLength(),
                    subListIcon = listIcon,
                    subListAction = onClick,
                    primaryItem = SubItemInfo(
                        text = title,
                        tapAction = onClick
                    ),
                    subtitleItem = subtitle?.let {
                        SubItemInfo(
                            text = it,
                            icon = icon,
                            tapAction = onClick
                        )
                    },
                )
            )
        }

        /**
         *  Clips items to 25 chars. It's for your own good, the native UI goes wild with really
         *  long text. When it fixes itself, the length is ~22 chars on a P6P.
         */
        private fun List<Text>.clipToLength() = map {
            it.apply { text = text.take(LIST_ITEM_MAX_LENGTH) }
        }
    }

    /**
     *  Full size target, showing a title, subtitle and icon, and additionally a card prompt with
     *  a logo and text.
     */
    data class LoyaltyCard(
        val context: Context,
        val id: String,
        val componentName: ComponentName,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val cardIcon: Icon,
        val cardPrompt: Text,
        val imageScaleType: ImageView.ScaleType? = null,
        val imageWidth: Int? = null,
        val imageHeight: Int? = null,
        val onClick: TapAction?
    ): TargetTemplate() {

        companion object {
            const val KEY_IMAGE_BITMAP = "imageBitmap"
            const val KEY_IMAGE_SCALE_TYPE = "imageScaleType"
            const val KEY_IMAGE_LAYOUT_WIDTH = "imageLayoutWidth"
            const val KEY_IMAGE_LAYOUT_HEIGHT = "imageLayoutHeight"
        }

        override fun create(): SmartspaceTarget {
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    icon = icon?.icon,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                baseAction = SmartspaceAction(
                    id = "${id}_base",
                    title = "",
                    extras = bundleOf("cardPrompt" to cardPrompt.text.toString()).also {
                        it.putParcelable(KEY_IMAGE_BITMAP, cardIcon.toBitmap(context))
                        if(imageScaleType != null){
                            it.putString(KEY_IMAGE_SCALE_TYPE, imageScaleType.name)
                        }
                        if(imageWidth != null){
                            it.putInt(KEY_IMAGE_LAYOUT_WIDTH, imageWidth)
                        }
                        if(imageHeight != null){
                            it.putInt(KEY_IMAGE_LAYOUT_HEIGHT, imageHeight)
                        }
                    },
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                featureType = SmartspaceTarget.FEATURE_LOYALTY_CARD,
                componentName = componentName,
                templateData = SubCardTemplateData(
                    subCardText = cardPrompt,
                    subCardIcon = cardIcon,
                    subCardAction = onClick,
                    primaryItem = SubItemInfo(
                        text = title,
                        tapAction = onClick
                    ),
                    subtitleItem = subtitle?.let {
                        SubItemInfo(
                            text = it,
                            icon = icon,
                            tapAction = onClick
                        )
                    },
                )
            )
        }
    }

    /**
     *  Full size target taking a title, subtitle and icon, and additionally a rectangular bitmap.
     *
     *  Available [featureType]s are [SmartspaceTarget.FEATURE_COMMUTE_TIME] and
     *  [SmartspaceTarget.FEATURE_ETA_MONITORING], but can be ignored if you're using the target
     *  for something else.
     */
    data class Image(
        val context: Context,
        val id: String,
        val componentName: ComponentName,
        val featureType: Int = SmartspaceTarget.FEATURE_COMMUTE_TIME,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val image: Icon?,
        val onClick: TapAction?
    ): TargetTemplate() {

        companion object {
            const val EXTRA_IMAGE = "imageBitmap"
        }

        override fun create(): SmartspaceTarget {
            if(!FEATURE_ALLOWLIST_IMAGE.contains(featureType)){
                throw InvalidTemplateException("Feature type $featureType invalid for Image template")
            }
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    icon = icon?.icon,
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                baseAction = SmartspaceAction(
                    id = "${id}_base",
                    title = "",
                    extras = bundleOf(EXTRA_IMAGE to image?.toBitmap(context)),
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                featureType = featureType,
                componentName = componentName
            )
        }
    }

    /**
     *  Full size target, showing a title, subtitle and icon, as well as a complex view with
     *  several options, designed for use with a connected doorbell.
     *
     *  **Note:** The complex view does not show when the target is displayed on the home screen,
     *  unless the user is using the Smartspacer Widget which will do its best to display the state.
     *  It also has limited system support - Pixel Experience does **not** implement it in its
     *  system, so this will not work on the lock screen or in the Smartspace on devices running PE.
     *
     *  See [DoorbellState] for information on the available states and how they can be used.
     *
     *  Available [featureType]s are [SmartspaceTarget.FEATURE_DOORBELL] and
     *  [SmartspaceTarget.FEATURE_PACKAGE_TRACKING], but can be ignored if you are using the target
     *  for something else.
     */
    data class Doorbell(
        val id: String,
        val componentName: ComponentName,
        val featureType: Int = SmartspaceTarget.FEATURE_DOORBELL,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val doorbellState: DoorbellState,
        val onClick: TapAction?
    ): TargetTemplate() {

        companion object {
            const val KEY_LOADING_SCREEN_STATE = "loadingScreenState"
            const val KEY_LOADING_SCREEN_ICON = "loadingScreenIcon"
            const val KEY_LOADING_ICON_WIDTH = "loadingIconWidth"
            const val KEY_LOADING_ICON_HEIGHT = "loadingIconHeight"
            const val KEY_PROGRESS_BAR_VISIBLE = "progressBarVisible"
            const val KEY_IMAGE_RATIO_WIDTH = "imageRatioWidth"
            const val KEY_IMAGE_RATIO_HEIGHT = "imageRatioHeight"
            const val KEY_IMAGE_SCALE_TYPE = "imageScaleType"
            const val KEY_IMAGE_URI = "imageUri"
            const val KEY_IMAGE_LAYOUT_WIDTH = "imageLayoutWidth"
            const val KEY_IMAGE_LAYOUT_HEIGHT = "imageLayoutHeight"
            const val KEY_PROGRESS_BAR_WIDTH = "progressBarWidth"
            const val KEY_PROGRESS_BAR_HEIGHT = "progressBarHeight"
            const val KEY_TINT_LOADING_ICON = "tintLoadingIcon"
            const val KEY_IMAGE_BITMAP = "imageBitmap"
            const val KEY_FRAME_DURATION_MS = "frameDurationMs"
        }

        override fun create(): SmartspaceTarget {
            if(!FEATURE_ALLOWLIST_DOORBELL.contains(featureType)){
                throw InvalidTemplateException("Feature type $featureType invalid for Doorbell template")
            }
            val iconGrid = if(doorbellState is DoorbellState.ImageUri){
                doorbellState.imageUris.mapIndexed { index, uri ->
                    SmartspaceAction(
                        id = "${id}_grid_$index",
                        title = "",
                        extras = bundleOf(
                            KEY_IMAGE_URI to uri.toString()
                        ),
                        pendingIntent = onClick?.pendingIntent,
                        intent = onClick?.intent
                    ).apply {
                        launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                    }
                }
            }else null
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    icon = icon?.icon,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                baseAction = SmartspaceAction(
                    id = "${id}_base",
                    title = "",
                    extras = doorbellState.toBundle()
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                iconGrid = iconGrid ?: emptyList(),
                featureType = featureType,
                componentName = componentName
            )
        }
    }

    sealed class DoorbellState(val index: Int) {

        companion object {
            fun fromTarget(target: SmartspaceTarget): DoorbellState? {
                val extras = target.baseAction?.extras ?: return null
                return when(extras.getIntOrNull(KEY_LOADING_SCREEN_STATE)) {
                    1 -> {
                        if(extras.getBoolean(KEY_PROGRESS_BAR_VISIBLE)){
                            LoadingIndeterminate(extras)
                        }else{
                            val uris = target.iconGrid.mapNotNull {
                                it.extras?.getString(KEY_IMAGE_URI)?.let { uriString ->
                                    Uri.parse(uriString)
                                }
                            }
                            ImageUri(extras, uris)
                        }
                    }
                    2 -> Videocam(extras)
                    3 -> VideocamOff(extras)
                    4 -> Loading(extras)
                    else -> ImageBitmap(extras)
                }
            }

            private fun Bundle.getIntOrNull(key: String): Int? {
                getInt(key, Integer.MAX_VALUE).let {
                    return if(it == Int.MAX_VALUE) null else it
                }
            }
        }

        /**
         *  Shows a loading indeterminate progress bar.
         */
        data class LoadingIndeterminate(
            val width: Int? = null,
            val height: Int? = null,
            val ratioWidth: Int = 1,
            val ratioHeight: Int = 1
        ): DoorbellState(0) {

            @RestrictTo(RestrictTo.Scope.LIBRARY)
            constructor(bundle: Bundle): this(
                bundle.getIntOrNull(KEY_PROGRESS_BAR_WIDTH),
                bundle.getIntOrNull(KEY_PROGRESS_BAR_HEIGHT),
                bundle.getInt(KEY_IMAGE_RATIO_WIDTH),
                bundle.getInt(KEY_IMAGE_RATIO_HEIGHT)
            )

            override fun toBundle(): Bundle {
                return bundleOf(
                    KEY_LOADING_SCREEN_STATE to 1,
                    KEY_PROGRESS_BAR_VISIBLE to true,
                    KEY_IMAGE_RATIO_WIDTH to ratioWidth,
                    KEY_IMAGE_RATIO_HEIGHT to ratioHeight
                ).also {
                    if(width != null){
                        it.putInt(KEY_PROGRESS_BAR_WIDTH, width)
                    }
                    if(height != null){
                        it.putInt(KEY_PROGRESS_BAR_HEIGHT, height)
                    }
                }
            }
        }

        /**
         *  Shows a specified bitmap as a small icon, intended for use as a logo during loading.
         *  [tint] specifies whether it should be tinted with the device's accent colour.
         */
        data class Loading(
            val icon: Bitmap,
            val tint: Boolean,
            val width: Int? = null,
            val height: Int? = null,
            val showProgressBar: Boolean = false,
            val ratioWidth: Int = 1,
            val ratioHeight: Int = 1
        ): DoorbellState(1){

            @RestrictTo(RestrictTo.Scope.LIBRARY)
            constructor(bundle: Bundle): this(
                bundle.getParcelableCompat(KEY_LOADING_SCREEN_ICON, Bitmap::class.java)!!,
                bundle.getBoolean(KEY_TINT_LOADING_ICON),
                bundle.getIntOrNull(KEY_LOADING_ICON_WIDTH),
                bundle.getIntOrNull(KEY_LOADING_ICON_HEIGHT),
                bundle.getBoolean(KEY_PROGRESS_BAR_VISIBLE),
                bundle.getInt(KEY_IMAGE_RATIO_WIDTH),
                bundle.getInt(KEY_IMAGE_RATIO_HEIGHT)
            )

            override fun toBundle(): Bundle {
                return bundleOf(
                    KEY_LOADING_SCREEN_STATE to 4,
                    KEY_LOADING_SCREEN_ICON to icon,
                    KEY_TINT_LOADING_ICON to tint,
                    KEY_IMAGE_RATIO_WIDTH to ratioWidth,
                    KEY_IMAGE_RATIO_HEIGHT to ratioHeight,
                    KEY_PROGRESS_BAR_VISIBLE to showProgressBar
                ).also {
                    if(width != null){
                        it.putInt(KEY_LOADING_ICON_WIDTH, width)
                    }
                    if(height != null){
                        it.putInt(KEY_LOADING_ICON_HEIGHT, height)
                    }
                }
            }
        }

        /**
         *  Shows a video camera icon
         */
        data class Videocam(
            val width: Int? = null,
            val height: Int? = null,
            val ratioWidth: Int = 1,
            val ratioHeight: Int = 1
        ): DoorbellState(2){

            @RestrictTo(RestrictTo.Scope.LIBRARY)
            constructor(bundle: Bundle): this(
                bundle.getIntOrNull(KEY_LOADING_ICON_WIDTH),
                bundle.getIntOrNull(KEY_LOADING_ICON_HEIGHT),
                bundle.getInt(KEY_IMAGE_RATIO_WIDTH),
                bundle.getInt(KEY_IMAGE_RATIO_HEIGHT)
            )

            override fun toBundle(): Bundle {
                return bundleOf(
                    KEY_LOADING_SCREEN_STATE to 2,
                    KEY_IMAGE_RATIO_WIDTH to ratioWidth,
                    KEY_IMAGE_RATIO_HEIGHT to ratioHeight
                ).also {
                    if(width != null){
                        it.putInt(KEY_LOADING_ICON_WIDTH, width)
                    }
                    if(height != null){
                        it.putInt(KEY_LOADING_ICON_HEIGHT, height)
                    }
                }
            }
        }

        /**
         *  Shows a video camera icon with a strike through it, indicating disabled
         */
        data class VideocamOff(
            val width: Int? = null,
            val height: Int? = null,
            val ratioWidth: Int = 1,
            val ratioHeight: Int = 1
        ): DoorbellState(3){

            @RestrictTo(RestrictTo.Scope.LIBRARY)
            constructor(bundle: Bundle): this(
                bundle.getIntOrNull(KEY_LOADING_ICON_WIDTH),
                bundle.getIntOrNull(KEY_LOADING_ICON_HEIGHT),
                bundle.getInt(KEY_IMAGE_RATIO_WIDTH),
                bundle.getInt(KEY_IMAGE_RATIO_HEIGHT)
            )

            override fun toBundle(): Bundle {
                return bundleOf(
                    KEY_LOADING_SCREEN_STATE to 3,
                    KEY_IMAGE_RATIO_WIDTH to ratioWidth,
                    KEY_IMAGE_RATIO_HEIGHT to ratioHeight
                ).also {
                    if(width != null){
                        it.putInt(KEY_LOADING_ICON_WIDTH, width)
                    }
                    if(height != null){
                        it.putInt(KEY_LOADING_ICON_HEIGHT, height)
                    }
                }
            }
        }

        /**
         *  Shows a specified bitmap, up to full size
         */
        data class ImageBitmap(
            val bitmap: Bitmap,
            val imageScaleType: ImageView.ScaleType? = null,
            val imageWidth: Int? = null,
            val imageHeight: Int? = null
        ): DoorbellState(4) {

            @RestrictTo(RestrictTo.Scope.LIBRARY)
            constructor(bundle: Bundle): this(
                bundle.getParcelableCompat(KEY_IMAGE_BITMAP, Bitmap::class.java)
                    ?: Bitmap_createEmptyBitmap(),
                bundle.getString(KEY_IMAGE_SCALE_TYPE)?.let { ImageView.ScaleType.valueOf(it) },
                bundle.getIntOrNull(KEY_IMAGE_LAYOUT_WIDTH),
                bundle.getIntOrNull(KEY_IMAGE_LAYOUT_HEIGHT)
            )

            override fun toBundle(): Bundle {
                return bundleOf(
                    KEY_IMAGE_BITMAP to bitmap
                ).also {
                    if(imageScaleType != null){
                        it.putString(KEY_IMAGE_SCALE_TYPE, imageScaleType.name)
                    }
                    if(imageWidth != null){
                        it.putInt(KEY_IMAGE_LAYOUT_WIDTH, imageWidth)
                    }
                    if(imageHeight != null){
                        it.putInt(KEY_IMAGE_LAYOUT_HEIGHT, imageHeight)
                    }
                }
            }
        }

        /**
         *  Shows a list of images with given [imageUri] [Uri]s. Specify a [frameDurationMs] for
         *  the time between the images being displayed. Please note that when
         *  using the Smartspacer Widget, this will be capped at a minimum of 500ms and frames will
         *  be skipped to keep up.
         *
         *  **Important**: The image Uris here are **NOT** web URLs. You should provide a Uri that
         *  can be loaded by `ContentResolver.openInputStream()`, and proxy it with
         *  [Context.createSmartspacerProxyUri] to allow Smartspacer to load it.
         */
        class ImageUri(
            val frameDurationMs: Int,
            val imageUris: List<Uri>
        ): DoorbellState(5) {

            constructor(bundle: Bundle, imageUris: List<Uri>): this(
                bundle.getInt(KEY_FRAME_DURATION_MS),
                imageUris
            )

            override fun toBundle(): Bundle {
                return bundleOf(
                    KEY_LOADING_SCREEN_STATE to 1,
                    KEY_PROGRESS_BAR_VISIBLE to false,
                    KEY_FRAME_DURATION_MS to frameDurationMs
                )
            }
        }

        internal abstract fun toBundle(): Bundle
    }

    /**
     *  Full size target, shows a title, subtitle, icon and a set of images to be shown in a
     *  sequence. Similar to [DoorbellState.ImageUri], but accepts [AndroidIcon]s directly.
     *
     *  Requires Android 13 for native, no system implementation available on 12/12L (consider using
     *  [Doorbell] with [DoorbellState.ImageUri] if you need backwards compatibility)
     *
     *  [frameDurationMs]: The duration each [images] should be shown for. Please note that when
     *  using the Smartspacer Widget, this will be capped at a minimum of 500ms and frames will be
     *  skipped to keep up.
     *
     *  [imageDimensionRatio]: The aspect ratio to pass to the `ConstraintLayout`, follow
     *  `ConstraintLayout.LayoutParams` syntax for `dimensionRatio`.
     */
    data class Images(
        val id: String,
        val componentName: ComponentName,
        val context: Context,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val images: List<Icon>,
        val onClick: TapAction?,
        val imageClickIntent: Intent? = null,
        val imageClickPendingIntent: PendingIntent? = null,
        val frameDurationMs: Int? = null,
        val imageDimensionRatio: String? = null
    ): TargetTemplate() {

        companion object {
            const val GIF_FRAME_DURATION_MS = "GifFrameDurationMillis"
            const val IMAGE_DIMENSION_RATIO = "imageDimensionRatio"
        }

        override fun create(): SmartspaceTarget {
            val bitmap = images.firstOrNull()?.toBitmap(context)
            //If not provided, calculate the aspect ratio of the passed icon to apply to the view
            val aspectRatio = imageDimensionRatio ?: bitmap?.let {
                val factor = greatestCommonFactor(it.width, it.height)
                val widthRatio = it.width / factor
                val heightRatio = it.height / factor
                "$widthRatio:$heightRatio"
            }
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    icon = icon?.icon,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                featureType = SmartspaceTarget.FEATURE_UNDEFINED,
                componentName = componentName,
                templateData = SubImageTemplateData(
                    subImages = images,
                    subImageTexts = emptyList(), //Unused by system
                    subImageAction = TapAction(
                        id = "${id}_base",
                        intent = imageClickIntent,
                        pendingIntent = imageClickPendingIntent,
                        extras = Bundle().apply {
                            if(frameDurationMs != null) {
                                putInt(GIF_FRAME_DURATION_MS, frameDurationMs)
                            }
                            if(aspectRatio != null){
                                putString(IMAGE_DIMENSION_RATIO, aspectRatio)
                            }
                        }
                    ),
                    primaryItem = SubItemInfo(
                        text = title,
                        tapAction = TapAction(
                            id = "${id}_header",
                            intent = onClick?.intent,
                            pendingIntent = onClick?.pendingIntent
                        )
                    ),
                    subtitleItem = subtitle?.let {
                        SubItemInfo(
                            text = it,
                            icon = icon,
                            tapAction = onClick
                        )
                    },
                )
            )
        }
    }

    /**
     *  Full size target, shows a title, subtitle, icon and a horizontal set of up to 4 columns
     *  containing a header, icon and footer. Designed to be used as a weather forecast.
     */
    //Equivalent of FEATURE_WEATHER_ALERT, but that is broken on 12/12L, so no native support on 12
    data class Carousel(
        val id: String,
        val componentName: ComponentName,
        val title: Text,
        val subtitle: Text?,
        val icon: Icon?,
        val items: List<CarouselTemplateData.CarouselItem>,
        val onClick: TapAction?,
        val onCarouselClick: TapAction?,
        val subComplication: SmartspaceAction? = null
    ): TargetTemplate() {

        override fun create(): SmartspaceTarget {
            return SmartspaceTarget(
                smartspaceTargetId = id,
                headerAction = SmartspaceAction(
                    id = "${id}_header",
                    icon = icon?.icon,
                    title = title.text.toString(),
                    subtitle = subtitle?.text,
                    pendingIntent = onClick?.pendingIntent,
                    intent = onClick?.intent
                ).apply {
                    launchDisplayOnLockScreen = onClick?.shouldShowOnLockScreen ?: false
                },
                featureType = SmartspaceTarget.FEATURE_UNDEFINED,
                componentName = componentName,
                templateData = CarouselTemplateData(
                    carouselItems = items,
                    carouselAction = TapAction(
                        id = "${id}_carousel",
                        intent = onCarouselClick?.intent,
                        pendingIntent = onCarouselClick?.pendingIntent
                    ),
                    primaryItem = SubItemInfo(
                        text = title,
                        tapAction = TapAction(
                            id = "${id}_header",
                            intent = onClick?.intent,
                            pendingIntent = onClick?.pendingIntent
                        )
                    ),
                    subtitleItem = subtitle?.let {
                        SubItemInfo(
                            text = it,
                            icon = icon,
                            tapAction = TapAction(
                                id = "${id}_subtitle",
                                intent = onClick?.intent,
                                pendingIntent = onClick?.pendingIntent
                            )
                        )
                    },
                    subtitleSupplementalItem = subComplication?.subItemInfo
                )
            )
        }

    }



    /**
     *  Fully customisable Target layout, supported on Smartspacer and Native Smartspace on
     *  Android 15 QPR2 or Android 16+.
     *
     *  You must provide a fallback template, which will be shown when RemoteViews are not
     *  supported. The Smartspacer ID from this fallback will be used all the time.
     */
    data class RemoteViews(
        val remoteViews: android.widget.RemoteViews,
        val fallback: TargetTemplate
    ): TargetTemplate() {
        override fun create(): SmartspaceTarget {
            if(fallback is RemoteViews) {
                throw IllegalArgumentException("Fallback cannot also be RemoteViews template")
            }
            return fallback.create().apply {
                remoteViews = this@RemoteViews.remoteViews
            }
        }
    }

    protected fun Icon.toBitmap(context: Context): Bitmap? {
        return icon.loadDrawable(context)?.toBitmap()
    }

    // https://stackoverflow.com/a/27753947/1088334
    protected fun greatestCommonFactor(width: Int, height: Int): Int {
        return if (height == 0) width else greatestCommonFactor(height, width % height)
    }

    protected fun createBlankAction(): SmartspaceAction {
        return SmartspaceAction("", title = "")
    }

    abstract fun create(): SmartspaceTarget

    class InvalidTemplateException(message: String): RuntimeException(message)

}