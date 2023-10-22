package com.kieronquinn.app.smartspacer.sdk.model

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Parcelable
import android.os.Process
import android.os.UserHandle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.annotations.LimitedNativeSupport
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemInfo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.model.weather.WeatherData
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.getEnumList
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.putEnumList
import kotlinx.parcelize.Parcelize
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon as SubItemInfoIcon

/**
 *  A [SmartspaceAction] represents an action which can be taken by a user by tapping on either
 *  the title, the subtitle or on the icon. Supported instances are Intents, PendingIntents or a
 *  ShortcutInfo (by putting the ShortcutInfoId in the bundle). These actions can be called from
 *  another process or within the client process.
 *
 *  Clients can also receive conditional Intents/PendingIntents in the extras bundle which are
 *  supposed to be fired when the conditions are met. For example, a user can invoke a dismiss/block
 *  action on a game score card but the intention is to only block the team and not the entire
 *  feature.
 *
 *  [Original class](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/smartspace/SmartspaceAction.java)
 */
@Parcelize
data class SmartspaceAction(
    /**
     * A unique ID of this [SmartspaceAction]. Your app's package name will be prefixed to this
     * ID, to enforce uniqueness across plugins.
     **/
    val id: String,
    /**
     * An icon which can be displayed in the UI
     */
    val icon: Icon? = null,
    /**
     * Title associated with an action
     */
    val title: String,
    /**
     *  Subtitle associated with an action
     */
    var subtitle: CharSequence? = null,
    /**
     *  Content description of this action for screen readers
     */
    var contentDescription: CharSequence? = null,
    /**
     *  Click event [PendingIntent]
     */
    val pendingIntent: PendingIntent? = null,
    /**
     *  Click event [Intent]
     */
    val intent: Intent? = null,
    /**
     *  User handle to show the action for
     */
    val userHandle: UserHandle = Process.myUserHandle(),
    /**
     *  Extras for this action
     */
    var extras: Bundle = Bundle.EMPTY,
    /**
     *  Equivalent SubItemInfo for this action, used on Android 13+ if the target has template data
     */
    val subItemInfo: SubItemInfo? = null,
    /**
     *  Only show this Action on the specified Smartspace surfaces. Setting this overrides the
     *  user's setting to show it on a given surface, but will not force it to be shown if they have
     *  explicitly disabled displaying it on that surface. Make sure you use this wisely and
     *  communicate it properly in UI.
     */
    var limitToSurfaces: Set<UiSurface> = emptySet(),
    /**
     *  Whether to skip this Action's PendingIntent action when invoking, this is used to trick
     *  native Smartspace into accepting this Action's click action, but allows Smartspacer's
     *  widgets and clients to work properly. Only used if [pendingIntent] is set, and [intent] is
     *  not.
     */
    val skipPendingIntent: Boolean = false
): Parcelable {

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_ICON = "icon"
        private const val KEY_TITLE = "title"
        private const val KEY_SUBTITLE = "subtitle"
        private const val KEY_CONTENT_DESCRIPTION = "content_description"
        private const val KEY_PENDING_INTENT = "pending_intent"
        private const val KEY_INTENT = "intent"
        private const val KEY_USER_HANDLE = "user_handle"
        private const val KEY_EXTRAS = "extras"
        private const val KEY_SUB_ITEM_INFO = "sub_item_info"
        private const val KEY_LIMIT_TO_SURFACES = "limit_to_surfaces"
        private const val KEY_SKIP_PENDING_INTENT = "skip_pending_intent"

        //Undocumented extras from SystemUI & Pixel Launcher
        /**
         *  Allows showing the Target this is attached to on the lock screen - native Smartspace
         *  only
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val KEY_EXTRA_SHOW_ON_LOCKSCREEN = "show_on_lockscreen"

        /**
         *  Hides the Target's title on AoD - native Smartspace only
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val KEY_EXTRA_HIDE_TITLE_ON_AOD = "hide_title_on_aod"

        /**
         *  Hides the Target's subtitle on AoD - native Smartspace only
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val KEY_EXTRA_HIDE_SUBTITLE_ON_AOD = "hide_subtitle_on_aod"

        /**
         *  Adds an "About this content" option to the popup, launching this intent. Supported by
         *  native Smartspace and Smartspacer powered screens (home screen only).
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val KEY_EXTRA_ABOUT_INTENT = "explanation_intent"

        /**
         *  Adds a "Feedback" option to the popup, launching this intent. Supported by native
         *  Smartspace and Smartspacer powered screens (home screen only).
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val KEY_EXTRA_FEEDBACK_INTENT = "feedback_intent"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle) : this(
        id = bundle.getString(KEY_ID)!!,
        icon = bundle.getParcelableCompat(KEY_ICON, Icon::class.java),
        title = bundle.getString(KEY_TITLE)!!,
        subtitle = bundle.getCharSequence(KEY_SUBTITLE),
        contentDescription = bundle.getCharSequence(KEY_CONTENT_DESCRIPTION),
        pendingIntent = bundle.getParcelableCompat(KEY_PENDING_INTENT, PendingIntent::class.java),
        intent = bundle.getParcelableCompat(KEY_INTENT, Intent::class.java),
        userHandle = bundle.getParcelableCompat(KEY_USER_HANDLE, UserHandle::class.java)!!,
        extras = bundle.getBundle(KEY_EXTRAS) ?: Bundle.EMPTY,
        subItemInfo = bundle.getBundle(KEY_SUB_ITEM_INFO)?.let { SubItemInfo(it) },
        limitToSurfaces = bundle.getEnumList<UiSurface>(KEY_LIMIT_TO_SURFACES)?.toSet() ?: emptySet(),
        skipPendingIntent = bundle.getBoolean(KEY_SKIP_PENDING_INTENT, false)
    )

    constructor(action: SmartspaceAction): this(
        action.id,
        action.icon,
        action.title,
        action.subtitle,
        action.contentDescription,
        action.pendingIntent,
        action.intent,
        action.userHandle,
        action.extras,
        action.subItemInfo,
        action.limitToSurfaces
    )

    /**
     *  Whether the Intent and PendingIntent should skip the unlock logic when launched from
     *  the lock screen, allowing activities that can show on the lockscreen to do so
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var launchDisplayOnLockScreen
        get() = extras.getBoolean(KEY_EXTRA_SHOW_ON_LOCKSCREEN, false)
        set(value) = setLaunchDisplayOnLockScreenTo(value)

    /**
     *  Shows an "About this content" option in the long-press popup on the Target this Action is
     *  attached to
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var aboutIntent
        get() = extras.getParcelableCompat(KEY_EXTRA_ABOUT_INTENT, Intent::class.java)
        set(value) = setAboutIntentTo(value)

    /**
     *  Shows a "Feedback" option in the long-press popup on the Target this Action is attached to
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var feedbackIntent
        get() = extras.getParcelableCompat(KEY_EXTRA_FEEDBACK_INTENT, Intent::class.java)
        set(value) = setFeedbackIntentTo(value)

    /**
     *  Hides the Target this Action is attached to's title when on the AoD
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var hideTitleOnAod
        get() = extras.getBoolean(KEY_EXTRA_HIDE_TITLE_ON_AOD, false)
        set(value) = setHideTitleOnAodTo(value)

    /**
     *  Hides the Target this Action is attached to's subtitle when on the AoD
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var hideSubtitleOnAod
        get() = extras.getBoolean(KEY_EXTRA_HIDE_SUBTITLE_ON_AOD, false)
        set(value) = setHideSubtitleOnAodTo(value)

    /**
     *  Weather data extras for this Action, shown on the lock screen and AoD on Android 14 when
     *  some clock styles are selected. The first Complication in the list with this set will be
     *  used by the system.
     */
    @LimitedNativeSupport
    var weatherData
        get() = WeatherData.fromBundle(extras)
        set(value) = setWeatherDataTo(value)

    private fun setLaunchDisplayOnLockScreenTo(showOnLockScreen: Boolean) {
        extras = Bundle().apply {
            putAll(extras)
            putBoolean(KEY_EXTRA_SHOW_ON_LOCKSCREEN, showOnLockScreen)
        }
    }

    private fun setAboutIntentTo(intent: Intent?) {
        extras = Bundle().apply {
            putAll(extras)
            if(intent == null){
                remove(KEY_EXTRA_ABOUT_INTENT)
            }else {
                putParcelable(KEY_EXTRA_ABOUT_INTENT, intent)
            }
        }
    }

    private fun setFeedbackIntentTo(intent: Intent?) {
        extras = Bundle().apply {
            putAll(extras)
            if(intent == null){
                remove(KEY_EXTRA_FEEDBACK_INTENT)
            }else {
                putParcelable(KEY_EXTRA_FEEDBACK_INTENT, intent)
            }
        }
    }

    private fun setHideTitleOnAodTo(hideTitleOnAod: Boolean) {
        extras = Bundle().apply {
            putAll(extras)
            putBoolean(KEY_EXTRA_HIDE_TITLE_ON_AOD, hideTitleOnAod)
        }
    }

    private fun setHideSubtitleOnAodTo(hideSubtitleOnAod: Boolean) {
        extras = Bundle().apply {
            putAll(extras)
            putBoolean(KEY_EXTRA_HIDE_SUBTITLE_ON_AOD, hideSubtitleOnAod)
        }
    }

    private fun setWeatherDataTo(weatherData: WeatherData?) {
        extras = Bundle().apply {
            putAll(extras)
            if(weatherData != null){
                putAll(weatherData.toBundle())
            }else{
                WeatherData.clearExtras(this)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle(): Bundle {
        return bundleOf(
            KEY_ID to id,
            KEY_ICON to icon,
            KEY_TITLE to title,
            KEY_SUBTITLE to subtitle,
            KEY_CONTENT_DESCRIPTION to contentDescription,
            KEY_PENDING_INTENT to pendingIntent,
            KEY_INTENT to intent,
            KEY_USER_HANDLE to userHandle,
            KEY_EXTRAS to extras,
            KEY_SUB_ITEM_INFO to subItemInfo?.toBundle(),
            KEY_SKIP_PENDING_INTENT to skipPendingIntent
        ).also {
            it.putEnumList(KEY_LIMIT_TO_SURFACES, limitToSurfaces.toList())
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun generateSubItemInfo(): SubItemInfo {
        return SubItemInfo(
            text = subtitle?.let { Text(it) },
            icon = icon?.let {
                SubItemInfoIcon(
                    icon = icon,
                    contentDescription = contentDescription,
                    shouldTint = ComplicationTemplate.shouldTint(this)
                )
            },
            tapAction = TapAction(
                id = id,
                intent = intent,
                pendingIntent = pendingIntent,
                extras = extras
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if(other !is SmartspaceAction) return false
        if(other.id != id) return false
        if(other.title != title) return false
        if(other.subtitle != subtitle) return false
        if(other.contentDescription != contentDescription) return false
        if(other.limitToSurfaces != limitToSurfaces) return false
        if(other.skipPendingIntent != skipPendingIntent) return false
        //Intent & extras are not checked for equality as they do not have .equals()
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + limitToSurfaces.hashCode()
        result = 31 * result + skipPendingIntent.hashCode()
        return result
    }

}
