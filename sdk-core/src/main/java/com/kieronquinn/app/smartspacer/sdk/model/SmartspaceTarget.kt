package com.kieronquinn.app.smartspacer.sdk.model

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.os.Process
import android.os.UserHandle
import android.widget.RemoteViews
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.annotations.LimitedNativeSupport
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget.Companion.FEATURE_UNDEFINED
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget.Companion.FEATURE_UPCOMING_ALARM
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CombinedCardsTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.HeadToHeadTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubCardTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubListTemplateData
import com.kieronquinn.app.smartspacer.sdk.utils.getEnumList
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.putEnumList
import kotlinx.parcelize.Parcelize

/**
 *  A [SmartspaceTarget] is a data class which holds all properties necessary to inflate a
 *  smartspace card. It contains data and related metadata which is supposed to be utilized by
 *  smartspace clients based on their own UI/UX requirements. Some of the properties have
 *  [SmartspaceAction] as their type because they can have associated actions.
 *
 *  <p><b>NOTE: </b>
 *  If [widget] is set, it should be preferred over all other properties.
 *  Else, if [sliceUri] is set, it should be preferred over all other data properties.
 *  Otherwise, the instance should be treated as a data object.
 *
 *  [Original class](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/smartspace/SmartspaceTarget.java)
 */
@Parcelize
data class SmartspaceTarget(
    /**
     *  A unique ID of this [SmartspaceTarget]. Your app's package name will be prefixed to this
     *  ID, to enforce uniqueness across plugins.
     */
    val smartspaceTargetId: String,
    /**
     *  A [SmartspaceAction] for the header of the Smartspace card
     */
    var headerAction: SmartspaceAction? = null,
    /**
     *  A [SmartspaceAction] for the base action in the Smartspace card
     */
    var baseAction: SmartspaceAction? = null,
    /**
     *  A timestamp indicating when the card was created
     */
    var creationTimeMillis: Long = System.currentTimeMillis(),
    /**
     *  A timestamp indicating when the card should be removed from view, in case the service
     *  disconnects or restarts
     */
    var expiryTimeMillis: Long = Long.MAX_VALUE,
    /**
     *  A score assigned to a target. Not currently used, return your target(s) in the order they
     *  should be displayed, if you have multiple.
     */
    var score: Float = 0f,
    /**
     *  A list of [SmartspaceAction]s containing all action chips
     */
    var actionChips: List<SmartspaceAction> = emptyList(),
    /**
     *  A list of [SmartspaceAction]s containing all icons for the grid
     */
    var iconGrid: List<SmartspaceAction> = emptyList(),
    /**
     *  Indicates the feature type of the card, see [FEATURE_UNDEFINED] through
     *  [FEATURE_UPCOMING_ALARM]
     */
    val featureType: Int,
    /**
     *  Indicates whether the content is sensitive, certain UI surfaces may choose to skip rendering
     *  real content until the device is unlocked
     */
    var isSensitive: Boolean = false,
    /**
     *  Indicates if the UI should show this target in its expanded state. Not currently used.
     */
    var shouldShowExpanded: Boolean = false,
    /**
     *  A Notification key if the target was generated using a notification
     */
    var sourceNotificationKey: String? = null,
    /**
     *  [ComponentName] for this target
     */
    val componentName: ComponentName,
    /**
     *  [UserHandle] for this target
     */
    val userHandle: UserHandle = Process.myUserHandle(),
    /**
     *  Target Id of other [SmartspaceTarget]s if it is associated with this target. This
     *  association is added to tell the UI that a card would be more useful if displayed with the
     *  associated smartspace target. This field is supposed to be taken as a suggestion and the
     *  association can be ignored based on the situation in the UI. It is possible to have a one way
     *  card association. In other words, Card B can be associated with Card A but not the other way
     *  around.
     *
     *  Not currently used.
     */
    var associatedSmartspaceTargetId: String? = null,
    /**
     *  Slice [Uri] if this target is a slice (not currently used)
     */
    var sliceUri: Uri? = null,
    /**
     *  [AppWidgetProviderInfo] if this target is a widget (not currently used)
     */
    var widget: AppWidgetProviderInfo? = null,
    /**
     *  [RemoteViews] if this Target has RemoteViews
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var remoteViews: RemoteViews? = null,
    /**
     *  [BaseTemplateData] for the layout and format on Android 13+ (ignored on 12/12L)
     */
    var templateData: BaseTemplateData? = null,
    /**
     *  The state to show when this Target is displayed in Smartspacer's expanded view.
     *
     *  This is exclusive to Smartspacer, and does not impact Smartspaces in any launcher or widget.
     */
    var expandedState: ExpandedState? = null,
    /**
     *  Whether this Target can be dismissed. This will hide the dismiss option **where
     *  possible**. It is not possible to hide it in Native Smartspace, but in other cases this
     *  will be applied to all Targets except those using [SmartspaceTarget.FEATURE_WEATHER]
     *  as their [SmartspaceTarget.featureType]. Regardless of this setting, you should override
     *  onDismiss in your provider and return `false` if the Target cannot be dismissed. This will
     *  show a Toast message to the user if they try to dismiss a non-dismissible Target from Native
     *  Smartspace.
     */
    var canBeDismissed: Boolean = true,
    /**
     *  If possible, marks this Target as being able to take two Complications. This only works if
     *  the [featureType] is set to [FEATURE_UNDEFINED], otherwise it will be ignored.
     */
    var canTakeTwoComplications: Boolean = false,
    /**
     *  Hides this Target if it has no Complications attached to it. This is for use on Targets
     *  which are intended to take two Complications, such as Date or Greeting.
     */
    var hideIfNoComplications: Boolean = false,
    /**
     *  Only show this Target on the specified Smartspace surfaces. Setting this overrides the
     *  user's setting to show it on a given surface, but will not force it to be shown if they have
     *  explicitly disabled displaying it on that surface. Make sure you use this wisely and
     *  communicate it properly in UI.
     */
    var limitToSurfaces: Set<UiSurface> = emptySet()
): Parcelable {

    companion object {

        const val FEATURE_UNDEFINED = 0
        const val FEATURE_WEATHER = 1
        const val FEATURE_CALENDAR = 2
        const val FEATURE_COMMUTE_TIME = 3
        const val FEATURE_FLIGHT = 4
        const val FEATURE_TIPS = 5
        const val FEATURE_REMINDER = 6
        const val FEATURE_ALARM = 7
        const val FEATURE_ONBOARDING = 8
        const val FEATURE_SPORTS = 9
        const val FEATURE_WEATHER_ALERT = 10
        const val FEATURE_CONSENT = 11
        const val FEATURE_STOCK_PRICE_CHANGE = 12
        const val FEATURE_SHOPPING_LIST = 13
        const val FEATURE_LOYALTY_CARD = 14
        const val FEATURE_MEDIA = 15
        const val FEATURE_BEDTIME_ROUTINE = 16
        const val FEATURE_FITNESS_TRACKING = 17
        const val FEATURE_ETA_MONITORING = 18
        const val FEATURE_MISSED_CALL = 19
        const val FEATURE_PACKAGE_TRACKING = 20
        const val FEATURE_TIMER = 21
        const val FEATURE_STOPWATCH = 22
        const val FEATURE_UPCOMING_ALARM = 23
        const val FEATURE_GAS_STATION_PAYMENT = 24
        const val FEATURE_PAIRED_DEVICE_STATUS = 25
        const val FEATURE_DRIVING_MODE = 26
        const val FEATURE_SLEEP_SUMMARY = 27
        const val FEATURE_FLASHLIGHT = 28
        const val FEATURE_TIME_TO_LEAVE = 29
        const val FEATURE_DOORBELL = 30
        const val FEATURE_MEDIA_RESUME = 31
        const val FEATURE_CROSS_DEVICE_TIMER = 32
        const val FEATURE_SEVERE_WEATHER_ALERT = 33
        const val FEATURE_HOLIDAY_ALARMS = 34
        const val FEATURE_SAFETY_CHECK = 35
        const val FEATURE_MEDIA_HEADS_UP = 36
        const val FEATURE_STEP_COUNTING = 37
        const val FEATURE_EARTHQUAKE_ALERT = 38
        const val FEATURE_BLAZE_BUILD_PROCESS = 40
        const val FEATURE_EARTHQUAKE_OCCURRED = 41

        const val FEATURE_COMBINATION = -1
        const val FEATURE_COMBINATION_AT_STORE = -2

        private const val KEY_SMARTSPACE_TARGET_ID = "smartspace_target_id"
        private const val KEY_HEADER_ACTION = "header_action"
        private const val KEY_BASE_ACTION = "base_action"
        private const val KEY_CREATION_TIME_MILLIS = "creation_time_millis"
        private const val KEY_EXPIRY_TIME_MILLIS = "expiry_time_millis"
        private const val KEY_SCORE = "score"
        private const val KEY_ACTION_CHIPS = "action_chips"
        private const val KEY_ICON_GRID = "icon_grid"
        private const val KEY_FEATURE_TYPE = "feature_type"
        private const val KEY_IS_SENSITIVE = "is_sensitive"
        private const val KEY_SHOULD_SHOW_EXPANDED = "should_show_expanded"
        private const val KEY_SOURCE_NOTIFICATION_KEY = "source_notification_key"
        private const val KEY_COMPONENT_NAME = "component_name"
        private const val KEY_USER_HANDLE = "user_handle"
        private const val KEY_ASSOCIATED_SMARTSPACE_TARGET_ID = "associated_smartspace_target_id"
        private const val KEY_SLICE_URI = "slice_uri"
        private const val KEY_WIDGET = "widget"
        private const val KEY_REMOTE_VIEWS = "remote_views"
        private const val KEY_TEMPLATE_DATA = "template_data"
        private const val KEY_TEMPLATE_DATA_TYPE = "template_data_type"
        private const val KEY_EXPANDED_STATE = "expanded_state"
        private const val KEY_CAN_BE_DISMISSED = "can_be_dismissed"
        private const val KEY_CAN_TAKE_TWO_COMPLICATIONS = "can_take_two_complications"
        private const val KEY_HIDE_IF_NO_COMPLICATIONS = "hide_if_no_complications"
        private const val KEY_LIMIT_TO_SURFACES = "limit_to_surfaces"

        const val UI_TEMPLATE_UNDEFINED = 0

        /**
         *  Default template whose data is represented by [BaseTemplateData]. The default
         *  template is also a base card for the other types of templates.
         */
        const val UI_TEMPLATE_DEFAULT = 1

        /**
         *  Sub-image template whose data is represented by [SubImageTemplateData]
         */
        const val UI_TEMPLATE_SUB_IMAGE = 2

        /**
         *  Sub-list template whose data is represented by [SubListTemplateData]
         */
        const val UI_TEMPLATE_SUB_LIST = 3

        /**
         *  Carousel template whose data is represented by [CarouselTemplateData]
         */
        const val UI_TEMPLATE_CAROUSEL = 4

        /**
         *  Head-to-head template whose data is represented by [HeadToHeadTemplateData]
         */
        const val UI_TEMPLATE_HEAD_TO_HEAD = 5

        /**
         *  Combined-cards template whose data is represented by [CombinedCardsTemplateData]
         */
        const val UI_TEMPLATE_COMBINED_CARDS = 6

        /**
         *  Sub-card template whose data is represented by [SubCardTemplateData]
         */
        const val UI_TEMPLATE_SUB_CARD = 7

        /**
         *  Write the [BaseTemplateData] value, with the type of the template
         */
        private fun Bundle.writeTemplateData(templateData: BaseTemplateData?) {
            if(templateData == null) return
            val type = templateData::class.java.name
            putString(KEY_TEMPLATE_DATA_TYPE, type)
            putBundle(KEY_TEMPLATE_DATA, templateData.toBundle())
        }

        /**
         *  Read the [BaseTemplateData] value, using the type in the bundle. Since the type is lost
         *  when bundling, we re-create it via reflection with the included type. If the versions
         *  mismatch and the class doesn't exist, it will end up as null.
         */
        private fun Bundle.getTemplateData(): BaseTemplateData? {
            val type = getString(KEY_TEMPLATE_DATA_TYPE) ?: return null
            val bundle = getBundle(KEY_TEMPLATE_DATA) ?: return null
            val clazz = try {
                Class.forName(type) as? Class<out BaseTemplateData>
            }catch (e: ClassNotFoundException){
                null
            } ?: return null
            return clazz.getConstructor(Bundle::class.java).newInstance(bundle)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        smartspaceTargetId = bundle.getString(KEY_SMARTSPACE_TARGET_ID)!!,
        headerAction = bundle.getBundle(KEY_HEADER_ACTION)?.let { SmartspaceAction(it) },
        baseAction = bundle.getBundle(KEY_BASE_ACTION)?.let { SmartspaceAction(it) },
        creationTimeMillis = bundle.getLong(KEY_CREATION_TIME_MILLIS),
        expiryTimeMillis = bundle.getLong(KEY_EXPIRY_TIME_MILLIS),
        score = bundle.getFloat(KEY_SCORE),
        actionChips = bundle.getParcelableArrayListCompat<Bundle>(KEY_ACTION_CHIPS, Bundle::class.java)
            ?.map { SmartspaceAction(it) } ?: emptyList(),
        iconGrid = bundle.getParcelableArrayListCompat<Bundle>(KEY_ICON_GRID, Bundle::class.java)
            ?.map { SmartspaceAction(it) } ?: emptyList(),
        featureType = bundle.getInt(KEY_FEATURE_TYPE),
        isSensitive = bundle.getBoolean(KEY_IS_SENSITIVE),
        shouldShowExpanded = bundle.getBoolean(KEY_SHOULD_SHOW_EXPANDED),
        sourceNotificationKey = bundle.getString(KEY_SOURCE_NOTIFICATION_KEY),
        componentName = bundle.getParcelableCompat(KEY_COMPONENT_NAME, ComponentName::class.java)!!,
        userHandle = bundle.getParcelableCompat(KEY_USER_HANDLE, UserHandle::class.java)!!,
        associatedSmartspaceTargetId = bundle.getString(KEY_ASSOCIATED_SMARTSPACE_TARGET_ID),
        sliceUri = bundle.getParcelableCompat(KEY_SLICE_URI, Uri::class.java),
        widget = bundle.getParcelableCompat(KEY_WIDGET, AppWidgetProviderInfo::class.java),
        remoteViews = bundle.getParcelableCompat(KEY_REMOTE_VIEWS, RemoteViews::class.java),
        templateData = bundle.getTemplateData(),
        expandedState = bundle.getBundle(KEY_EXPANDED_STATE)?.let { ExpandedState(it) },
        canBeDismissed = bundle.getBoolean(KEY_CAN_BE_DISMISSED),
        canTakeTwoComplications = bundle.getBoolean(KEY_CAN_TAKE_TWO_COMPLICATIONS),
        hideIfNoComplications = bundle.getBoolean(KEY_HIDE_IF_NO_COMPLICATIONS),
        limitToSurfaces = bundle.getEnumList<UiSurface>(KEY_LIMIT_TO_SURFACES)?.toSet() ?: emptySet()
    )

    constructor(target: SmartspaceTarget): this(
        target.smartspaceTargetId,
        target.headerAction,
        target.baseAction,
        target.creationTimeMillis,
        target.expiryTimeMillis,
        target.score,
        target.actionChips,
        target.iconGrid,
        target.featureType,
        target.isSensitive,
        target.shouldShowExpanded,
        target.sourceNotificationKey,
        target.componentName,
        target.userHandle,
        target.associatedSmartspaceTargetId,
        target.sliceUri,
        target.widget,
        target.remoteViews,
        target.templateData,
        target.expandedState,
        target.canBeDismissed,
        target.canTakeTwoComplications,
        target.hideIfNoComplications,
        target.limitToSurfaces
    )

    /**
     *  Shows an "About this content" option in the long-press popup on the Target, when supported.
     *  Support for this is limited to the Pixel Launcher & screens where Smartspacer is providing
     *  the UI (including launchers using the plugin and Expanded Smartspace). When tapped, this
     *  intent will be launched.
     */
    @LimitedNativeSupport
    var aboutIntent
        get() = baseAction?.aboutIntent
        set(value) {
            val base = baseAction
                ?: throw RuntimeException("This Target does not support setting an About intent")
            base.aboutIntent = value
        }

    /**
     *  Shows a "Feedback" option in the long-press popup on the Target, when supported. Support
     *  for this is limited to the Pixel Launcher & screens where Smartspacer is providing the UI
     *  (including launchers using the plugin and Expanded Smartspace). When tapped, this intent
     *  will be launched.
     */
    @LimitedNativeSupport
    var feedbackIntent
        get() = baseAction?.feedbackIntent
        set(value) {
            val base = baseAction
                ?: throw RuntimeException("This Target does not support setting an feedback intent")
            base.feedbackIntent = value
        }

    /**
     *  Hides this Target's title when showing on the Always on Display. This only works in Native
     *  Smartspace, and may not be supported by all OEMs.
     */
    @LimitedNativeSupport
    var hideTitleOnAod
        get() = baseAction?.hideTitleOnAod ?: false
        set(value) {
            val base = baseAction
                ?: throw RuntimeException("This Target does not support hiding the title on the AoD")
            base.hideTitleOnAod = value
        }

    /**
     *  Hides this Target's subtitle when showing on the Always on Display. This only works in
     *  Native Smartspace, and may not be supported by all OEMs.
     */
    @LimitedNativeSupport
    var hideSubtitleOnAod
        get() = baseAction?.hideSubtitleOnAod ?: false
        set(value) {
            val base = baseAction
                ?: throw RuntimeException("This Target does not support hiding the subtitle on the AoD")
            base.hideSubtitleOnAod = value
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle(): Bundle {
        return bundleOf(
            KEY_SMARTSPACE_TARGET_ID to smartspaceTargetId,
            KEY_HEADER_ACTION to headerAction?.toBundle(),
            KEY_BASE_ACTION to baseAction?.toBundle(),
            KEY_CREATION_TIME_MILLIS to creationTimeMillis,
            KEY_EXPIRY_TIME_MILLIS to expiryTimeMillis,
            KEY_SCORE to score,
            KEY_ACTION_CHIPS to ArrayList(actionChips.map { it.toBundle() }),
            KEY_ICON_GRID to ArrayList(iconGrid.map { it.toBundle() }),
            KEY_FEATURE_TYPE to featureType,
            KEY_IS_SENSITIVE to isSensitive,
            KEY_SHOULD_SHOW_EXPANDED to shouldShowExpanded,
            KEY_SOURCE_NOTIFICATION_KEY to sourceNotificationKey,
            KEY_COMPONENT_NAME to componentName,
            KEY_USER_HANDLE to userHandle,
            KEY_ASSOCIATED_SMARTSPACE_TARGET_ID to associatedSmartspaceTargetId,
            KEY_SLICE_URI to sliceUri,
            KEY_WIDGET to widget,
            KEY_REMOTE_VIEWS to remoteViews,
            KEY_EXPANDED_STATE to expandedState?.toBundle(),
            KEY_CAN_BE_DISMISSED to canBeDismissed,
            KEY_CAN_TAKE_TWO_COMPLICATIONS to canTakeTwoComplications,
            KEY_HIDE_IF_NO_COMPLICATIONS to hideIfNoComplications
        ).also {
            it.writeTemplateData(templateData)
            it.putEnumList(KEY_LIMIT_TO_SURFACES, limitToSurfaces.toList())
        }
    }

    override fun equals(other: Any?): Boolean {
        if(other !is SmartspaceTarget) return false
        if(other.smartspaceTargetId != smartspaceTargetId) return false
        if(other.headerAction != headerAction) return false
        if(other.baseAction != baseAction) return false
        if(other.creationTimeMillis != creationTimeMillis) return false
        if(other.expiryTimeMillis != expiryTimeMillis) return false
        if(other.score != score) return false
        if(other.actionChips != actionChips) return false
        if(other.iconGrid != iconGrid) return false
        if(other.featureType != featureType) return false
        if(other.isSensitive != isSensitive) return false
        if(other.shouldShowExpanded != shouldShowExpanded) return false
        if(other.sourceNotificationKey != sourceNotificationKey) return false
        if(other.componentName != componentName) return false
        if(other.userHandle != userHandle) return false
        if(other.associatedSmartspaceTargetId != associatedSmartspaceTargetId) return false
        if(other.sliceUri != sliceUri) return false
        if(other.widget != widget) return false
        if(other.templateData != templateData) return false
        if(other.expandedState != expandedState) return false
        if(other.canBeDismissed != canBeDismissed) return false
        if(other.canTakeTwoComplications != canTakeTwoComplications) return false
        if(other.hideIfNoComplications != hideIfNoComplications) return false
        if(other.limitToSurfaces != limitToSurfaces) return false
        return true
    }

    fun equalsForUi(other: Any?): Boolean {
        if(other !is SmartspaceTarget) return false
        if(other.smartspaceTargetId != smartspaceTargetId) return false
        if(other.headerAction != headerAction) return false
        if(other.baseAction != baseAction) return false
        if(other.actionChips != actionChips) return false
        if(other.iconGrid != iconGrid) return false
        if(other.featureType != featureType) return false
        if(other.isSensitive != isSensitive) return false
        if(other.shouldShowExpanded != shouldShowExpanded) return false
        if(other.sourceNotificationKey != sourceNotificationKey) return false
        if(other.componentName != componentName) return false
        if(other.userHandle != userHandle) return false
        if(other.associatedSmartspaceTargetId != associatedSmartspaceTargetId) return false
        if(other.sliceUri != sliceUri) return false
        if(other.widget != widget) return false
        if(other.templateData != templateData) return false
        if(other.canBeDismissed != canBeDismissed) return false
        if(other.canTakeTwoComplications != canTakeTwoComplications) return false
        if(other.hideIfNoComplications != hideIfNoComplications) return false
        if(other.limitToSurfaces != limitToSurfaces) return false
        return true
    }

    override fun hashCode(): Int {
        var result = smartspaceTargetId.hashCode()
        result = 31 * result + (headerAction?.hashCode() ?: 0)
        result = 31 * result + (baseAction?.hashCode() ?: 0)
        result = 31 * result + creationTimeMillis.hashCode()
        result = 31 * result + expiryTimeMillis.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + actionChips.hashCode()
        result = 31 * result + iconGrid.hashCode()
        result = 31 * result + featureType
        result = 31 * result + isSensitive.hashCode()
        result = 31 * result + shouldShowExpanded.hashCode()
        result = 31 * result + (sourceNotificationKey?.hashCode() ?: 0)
        result = 31 * result + componentName.hashCode()
        result = 31 * result + userHandle.hashCode()
        result = 31 * result + (associatedSmartspaceTargetId?.hashCode() ?: 0)
        result = 31 * result + (sliceUri?.hashCode() ?: 0)
        result = 31 * result + (widget?.hashCode() ?: 0)
        result = 31 * result + (templateData?.hashCode() ?: 0)
        result = 31 * result + (expandedState?.hashCode() ?: 0)
        result = 31 * result + canBeDismissed.hashCode()
        result = 31 * result + canTakeTwoComplications.hashCode()
        result = 31 * result + hideIfNoComplications.hashCode()
        result = 31 * result + limitToSurfaces.hashCode()
        return result
    }

    fun hashCodeForUi(): Int {
        var result = smartspaceTargetId.hashCode()
        result = 31 * result + (headerAction?.hashCode() ?: 0)
        result = 31 * result + (baseAction?.hashCode() ?: 0)
        result = 31 * result + creationTimeMillis.hashCode()
        result = 31 * result + expiryTimeMillis.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + actionChips.hashCode()
        result = 31 * result + iconGrid.hashCode()
        result = 31 * result + featureType
        result = 31 * result + isSensitive.hashCode()
        result = 31 * result + shouldShowExpanded.hashCode()
        result = 31 * result + (sourceNotificationKey?.hashCode() ?: 0)
        result = 31 * result + componentName.hashCode()
        result = 31 * result + userHandle.hashCode()
        result = 31 * result + (associatedSmartspaceTargetId?.hashCode() ?: 0)
        result = 31 * result + (sliceUri?.hashCode() ?: 0)
        result = 31 * result + (widget?.hashCode() ?: 0)
        result = 31 * result + (templateData?.hashCode() ?: 0)
        result = 31 * result + canBeDismissed.hashCode()
        result = 31 * result + canTakeTwoComplications.hashCode()
        result = 31 * result + hideIfNoComplications.hashCode()
        result = 31 * result + limitToSurfaces.hashCode()
        return result
    }

}
