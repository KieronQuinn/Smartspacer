package com.kieronquinn.app.smartspacer.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemProperties
import androidx.annotation.StringRes
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepository.SmartspacerSetting
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedBackground
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedHideAddButton
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TargetCountLimit
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.utils.extensions.getSystemHideSensitive
import com.kieronquinn.app.smartspacer.utils.extensions.isAtLeastU
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

interface SmartspacerSettingsRepository {

    /**
     *  Whether the user has completed the setup process
     */
    @IgnoreInBackup
    val hasSeenSetup: SmartspacerSetting<Boolean>

    /**
     *  The user's name, which is loaded when the Shizuku service is started and used as the default
     *  in the Greeting target.
     */
    val userName: SmartspacerSetting<String>

    /**
     *  Whether we *know* that Allow unrestricted settings has been pressed - the system does not
     *  allow us to check, so this should be switched when an action that requires unrestricted
     *  happens. Will prevent the UI telling the user to allow unrestricted settings appearing again
     */
    @IgnoreInBackup
    val isRestrictedModeDisabled: SmartspacerSetting<Boolean>

    /**
     *  Whether Enhanced Mode (Native Smartspace + System Smartspace Targets/Complications) is
     *  enabled - this is gated by a successful Shizuku response, it will only be enabled after
     *  that. A denied response resets this, but a failure does not (a notification to start
     *  Shizuku will be shown instead). It's used to know when to automatically attempt to connect
     *  to System Smartspace and begin the native setup process (if enabled).
     */
    @IgnoreInBackup
    val enhancedMode: SmartspacerSetting<Boolean>

    /**
     *  Whether the native Smartspace service has successfully started. This is used to show
     *  notifications to remind users to re-enable the service.
     */
    @IgnoreInBackup
    val hasUsedNativeMode: SmartspacerSetting<Boolean>

    /**
     *  The count to limit the number of native targets sent to system Smartspace Sessions.
     */
    @IgnoreInBackup
    val nativeTargetCountLimit: SmartspacerSetting<TargetCountLimit>

    /**
     *  Whether to hide incompatible targets from Native Smartspace rather than just showing their
     *  title and subtitle
     */
    val nativeHideIncompatible: SmartspacerSetting<Boolean>

    /**
     *  Whether to forward the Media Data Manager suggestions from the system.
     */
    val nativeShowMediaSuggestions: SmartspacerSetting<Boolean>

    /**
     *  Whether the user has Split Smartspace enabled. Defaults to system value, but can be disabled,
     *  for example if the user is using a clock style where this is not beneficial.
     */
    @IgnoreInBackup
    val nativeUseSplitSmartspace: SmartspacerSetting<Boolean>

    /**
     *  If enabled, will attempt to start/restart the native Smartspace Service rather than showing
     *  the notification. This will cause unprompted SystemUI restarts, so disabled by default.
     */
    val nativeImmediateStart: SmartspacerSetting<Boolean>

    /**
     *  Hides targets marked as sensitive's content on the Lock Screen UI surface
     */
    val hideSensitive: SmartspacerSetting<HideSensitive>

    /**
     *  Whether Expanded Mode is enabled at all
     */
    val expandedModeEnabled: SmartspacerSetting<Boolean>

    /**
     *  Whether to show the header in the expanded screen
     */
    val expandedShowHeader: SmartspacerSetting<Boolean>

    /**
     *  Whether to show the search box on the expanded screen
     */
    val expandedShowSearchBox: SmartspacerSetting<Boolean>

    /**
     *  The package for the expanded search box to open, if enabled
     */
    val expandedSearchPackage: SmartspacerSetting<String>

    /**
     *  Whether to show today's Google Doodle on the expanded screen
     */
    val expandedShowDoodle: SmartspacerSetting<Boolean>

    /**
     *  The [ExpandedOpenMode] to use when a Target is clicked on the Home Screen
     */
    val expandedOpenModeHome: SmartspacerSetting<ExpandedOpenMode>

    /**
     *  The [ExpandedOpenMode] to use when a Target is clicked on the Lock Screen
     */
    val expandedOpenModeLock: SmartspacerSetting<ExpandedOpenMode>

    /**
     *  Whether to close Expanded Smartspace when the device is locked
     */
    val expandedCloseWhenLocked: SmartspacerSetting<Boolean>

    /**
     *  Whether to enable background blur on the Expanded Smartspace window
     */
    @Deprecated("No longer set in the settings")
    val expandedBlurBackground: SmartspacerSetting<Boolean>

    /**
     *  The background to use behind Expanded Smartspace
     */
    val expandedBackground: SmartspacerSetting<ExpandedBackground>

    /**
     *  The mode to use when tinting Expanded Smartspace
     */
    val expandedTintColour: SmartspacerSetting<TintColour>

    /**
     *  Whether the user has previously clicked "Add Widget", if set this will minimise the button
     *  to be less visually disruptive
     */
    val expandedHasClickedAdd: SmartspacerSetting<Boolean>

    /**
     *  Whether to force the use of Google Sans in the widgets on the Expanded Smartspace
     */
    val expandedWidgetUseGoogleSans: SmartspacerSetting<Boolean>

    /**
     *  If/when to hide the add widgets button in Expanded Smartspace
     */
    val expandedHideAddButton: SmartspacerSetting<ExpandedHideAddButton>

    /**
     *  Whether to allow multiple columns in Expanded Smartspace
     */
    val expandedMultiColumnEnabled: SmartspacerSetting<Boolean>

    /**
     *  Whether the Xposed module should replace Discover with Expanded Smartspace
     */
    val expandedXposedEnabled: SmartspacerSetting<Boolean>

    /**
     *  Whether to put the Complications above Targets in Expanded Smartspace
     */
    val expandedComplicationsFirst: SmartspacerSetting<Boolean>

    /**
     *  Whether to show the shadow on light text in Expanded Smartspace
     */
    val expandedShowShadow: SmartspacerSetting<Boolean>

    /**
     *  Whether the OEM Smartspace Service should be enabled
     */
    @IgnoreInBackup //Ignored as we can't guarantee permission will be kept
    val oemSmartspaceEnabled: SmartspacerSetting<Boolean>

    /**
     *  Whether to hide incompatible targets in OEM Smartspace
     */
    val oemHideIncompatible: SmartspacerSetting<Boolean>

    /**
     *  Whether Smartspacer's update check is enabled
     */
    val updateCheckEnabled: SmartspacerSetting<Boolean>

    /**
     *  Whether the Smartspacer Plugin Repository is enabled
     */
    val pluginRepositoryEnabled: SmartspacerSetting<Boolean>

    /**
     *  Whether the Smartspacer Plugin Repository should automatically check for updates
     */
    val pluginRepositoryUpdateCheckEnabled: SmartspacerSetting<Boolean>

    /**
     *  plugins.json URL for the Smartspacer Plugin Repository
     */
    val pluginRepositoryUrl: SmartspacerSetting<String>

    /**
     *  The wallpaper colour to use (< Android 12)
     */
    @IgnoreInBackup
    val monetColor: SmartspacerSetting<Int>

    /**
     *  The timestamp of the first open of the app after install (or after data clear)
     */
    @IgnoreInBackup
    val installTime: SmartspacerSetting<Long>

    /**
     *  Whether the donate prompt is enabled (defaults to enabled, can be disabled permanently from
     *  the donate screen)
     */
    @IgnoreInBackup
    val donatePromptEnabled: SmartspacerSetting<Boolean>

    /**
     *  The timestamp that the donate prompt was last dismissed at
     */
    @IgnoreInBackup
    val donatePromptDismissedAt: SmartspacerSetting<Long>

    /**
     *  Whether analytics & crash reporting is enabled
     */
    @IgnoreInBackup
    val analyticsEnabled: SmartspacerSetting<Boolean>

    /**
     *  Whether to prompt the user to enable Display Over Other apps when Smartspacer launches,
     *  since the overlay is unable to launch it itself (ironically until this is granted)
     */
    @IgnoreInBackup
    val requiresDisplayOverOtherAppsPermission: SmartspacerSetting<Boolean>

    /**
     *  Whether the Lock Screen Notification "Widget" is enabled
     */
    val notificationWidgetServiceEnabled: SmartspacerSetting<Boolean>

    /**
     *  The mode of tinting for the Lock Screen Notification "Widget"
     */
    val notificationWidgetTintColour: SmartspacerSetting<TintColour>

    enum class TargetCountLimit(val count: Int, @StringRes val label: Int, @StringRes val content: Int) {
        ONE(
            1,
            R.string.native_mode_settings_target_limit_one,
            R.string.native_mode_settings_target_limit_one_content
        ),
        AUTOMATIC(
            -1,
            R.string.native_mode_settings_target_limit_automatic,
            R.string.native_mode_settings_target_limit_automatic_content
        ),
        UNLIMITED(
            Int.MAX_VALUE,
            R.string.native_mode_settings_target_limit_unlimited,
            R.string.native_mode_settings_target_limit_unlimited_content
        )
    }

    enum class HideSensitive(@StringRes val label: Int, @StringRes val content: Int) {
        DISABLED(
            R.string.settings_hide_sensitive_contents_disabled,
            R.string.settings_hide_sensitive_contents_disabled_content
        ),
        HIDE_CONTENTS(
            R.string.settings_hide_sensitive_contents_hide_contents,
            R.string.settings_hide_sensitive_contents_hide_contents_content
        ),
        HIDE_TARGET(
            R.string.settings_hide_sensitive_contents_hide,
            R.string.settings_hide_sensitive_contents_hide_content
        )
    }

    enum class ExpandedOpenMode(@StringRes val label: Int, @StringRes val content: Int) {
        NEVER(
            R.string.expanded_settings_open_mode_never_title,
            R.string.expanded_settings_open_mode_never_content
        ),
        IF_HAS_EXTRAS(
            R.string.expanded_settings_open_mode_if_has_extras_title,
            R.string.expanded_settings_open_mode_if_has_extras_content
        ),
        ALWAYS(
            R.string.expanded_settings_open_mode_always_title,
            R.string.expanded_settings_open_mode_always_content
        )
    }

    enum class TintColour(@StringRes val label: Int, @StringRes val labelAlt: Int) {
        AUTOMATIC(R.string.tint_colour_automatic, R.string.tint_colour_automatic_alt),
        WHITE(R.string.tint_colour_white, R.string.tint_colour_white),
        BLACK(R.string.tint_colour_black, R.string.tint_colour_black)
    }

    enum class ExpandedBackground(@StringRes val label: Int) {
        SCRIM(R.string.expanded_settings_background_mode_scrim),
        BLUR(R.string.expanded_settings_background_mode_blur),
        SOLID(R.string.expanded_settings_background_mode_solid);

        companion object {
            fun getAvailable(isBlurCompatible: Boolean): List<ExpandedBackground> {
                return entries.filter { it != BLUR || isBlurCompatible }
            }
        }
    }

    enum class ExpandedHideAddButton(@StringRes val label: Int) {
        NEVER(R.string.expanded_settings_hide_add_button_never),
        OVERLAY_ONLY(R.string.expanded_settings_hide_add_button_overlay_only),
        ALWAYS(R.string.expanded_settings_hide_add_button_always),
    }

    suspend fun setRestrictedModeKnownDisabledIfNeeded()
    suspend fun setInstallTimeIfNeeded()
    suspend fun getBackup(): Map<String, String>
    suspend fun restoreBackup(settings: Map<String, String>)

}

class SmartspacerSettingsRepositoryImpl(
    context: Context
): BaseSettingsRepositoryImpl(), SmartspacerSettingsRepository {

    companion object {
        private const val SHARED_PREFS_NAME = "${BuildConfig.APPLICATION_ID}_shared_prefs"
        private const val SHARED_PREFS_VERSION = 2

        private const val KEY_SHARED_PREFS_VERSION = "shared_prefs_version"

        private const val KEY_HAS_SEEN_SETUP = "has_seen_setup"
        private const val DEFAULT_HAS_SEEN_SETUP = false

        private const val KEY_USER_NAME = "user_name"
        private const val DEFAULT_USER_NAME = ""

        private const val KEY_IS_RESTRICTED_MODE_DISABLED = "is_restricted_mode_disabled"
        private const val DEFAULT_IS_RESTRICTED_MODE_DISABLED = false

        private const val KEY_ENHANCED_MODE = "enhanced_mode"
        private const val DEFAULT_ENHANCED_MODE = false

        private const val KEY_HAS_USED_NATIVE_MODE = "has_used_native_mode"
        private const val DEFAULT_HAS_USED_NATIVE_MODE = false

        private const val KEY_NATIVE_TARGET_COUNT = "native_target_count"
        private val DEFAULT_NATIVE_TARGET_COUNT = TargetCountLimit.UNLIMITED

        private const val KEY_NATIVE_HIDE_INCOMPATIBLE = "native_hide_incompatible"
        private const val DEFAULT_NATIVE_HIDE_INCOMPATIBLE = false

        private const val KEY_NATIVE_SHOW_MEDIA_SUGGESTIONS = "native_show_media_suggestions"
        private const val DEFAULT_NATIVE_SHOW_MEDIA_SUGGESTIONS = true

        private const val KEY_NATIVE_USE_SPLIT_SMARTSPACE = "native_use_split_smartspace"

        private const val KEY_NATIVE_IMMEDIATE_START = "native_immediate_start"
        private const val DEFAULT_NATIVE_IMMEDIATE_START = false

        private const val KEY_HIDE_SENSITIVE = "hide_sensitive"

        private const val KEY_EXPANDED_MODE_ENABLED = "expanded_mode_enabled"
        private const val DEFAULT_EXPANDED_MODE_ENABLED = true

        private const val KEY_EXPANDED_SHOW_HEADER = "expanded_show_header"
        private const val DEFAULT_EXPANDED_SHOW_HEADER = true

        private const val KEY_EXPANDED_SHOW_SEARCH_BOX = "expanded_show_search_box"
        private const val DEFAULT_EXPANDED_SHOW_SEARCH_BOX = false

        private const val KEY_EXPANDED_SEARCH_PACKAGE = "expanded_search_package"
        private const val DEFAULT_EXPANDED_SEARCH_PACKAGE = ""

        private const val KEY_EXPANDED_SHOW_DOODLE = "expanded_show_doodle"
        private const val DEFAULT_EXPANDED_SHOW_DOODLE = false

        private const val KEY_EXPANDED_OPEN_MODE_HOME = "expanded_open_mode_home"
        private val DEFAULT_EXPANDED_OPEN_MODE_HOME = ExpandedOpenMode.IF_HAS_EXTRAS

        private const val KEY_EXPANDED_OPEN_MODE_LOCK = "expanded_open_mode_lock"
        private val DEFAULT_EXPANDED_OPEN_MODE_LOCK = ExpandedOpenMode.IF_HAS_EXTRAS

        private const val KEY_EXPANDED_CLOSE_WHEN_LOCKED = "expanded_close_when_locked"
        private const val DEFAULT_EXPANDED_CLOSE_WHEN_LOCKED = false

        private const val KEY_EXPANDED_BACKGROUND_BLUR = "expanded_background_blur"
        private const val DEFAULT_EXPANDED_BACKGROUND_BLUR = false

        private const val KEY_EXPANDED_BACKGROUND = "expanded_background"

        private const val KEY_EXPANDED_TINT_COLOUR = "expanded_tint_colour"
        private val DEFAULT_EXPANDED_TINT_COLOUR = TintColour.AUTOMATIC

        private const val KEY_EXPANDED_HAS_CLICKED_ADD = "expanded_has_clicked_add"
        private const val DEFAULT_EXPANDED_HAS_CLICKED_ADD = false

        private const val KEY_EXPANDED_WIDGETS_USE_GOOGLE_SANS = "expanded_widgets_use_google_sans"
        private const val DEFAULT_EXPANDED_WIDGETS_USE_GOOGLE_SANS = false

        private const val KEY_EXPANDED_HIDE_ADD_BUTTON = "expanded_hide_add_button"
        private val DEFAULT_EXPANDED_HIDE_ADD_BUTTON = ExpandedHideAddButton.NEVER

        private const val KEY_EXPANDED_MULTI_COLUMN_ENABLED = "expanded_multi_column_enabled"
        private const val DEFAULT_EXPANDED_MULTI_COLUMN_ENABLED = true

        private const val KEY_EXPANDED_XPOSED_ENABLED = "expanded_xposed_enabled"
        private const val DEFAULT_EXPANDED_XPOSED_ENABLED = false

        private const val KEY_EXPANDED_COMPLICATIONS_FIRST = "expanded_complications_first"
        private const val DEFAULT_EXPANDED_COMPLICATIONS_FIRST = false

        private const val KEY_EXPANDED_SHOW_SHADOW = "expanded_show_shadow"
        private const val DEFAULT_EXPANDED_SHOW_SHADOW = true

        private const val KEY_OEM_SMARTSPACE_ENABLED = "oem_smartspace_enabled"
        private const val DEFAULT_OEM_SMARTSPACE_ENABLED = false

        private const val KEY_OEM_HIDE_INCOMPATIBLE = "oem_hide_incompatible"
        private const val DEFAULT_OEM_HIDE_INCOMPATIBLE = false

        private const val KEY_UPDATE_CHECK_ENABLED = "update_check_enabled"
        private const val DEFAULT_UPDATE_CHECK_ENABLED = true

        private const val KEY_PLUGIN_REPOSITORY_ENABLED = "plugin_repository_enabled"
        private const val DEFAULT_PLUGIN_REPOSITORY_ENABLED = true

        private const val KEY_PLUGIN_REPOSITORY_UPDATE_CHECK_ENABLED = "plugin_repository_update_check_enabled"
        private const val DEFAULT_PLUGIN_REPOSITORY_UPDATE_CHECK_ENABLED = true

        private const val KEY_PLUGIN_REPOSITORY_URL = "plugin_repository_url"
        private const val DEFAULT_PLUGIN_REPOSITORY_URL = "https://raw.githubusercontent.com/KieronQuinn/SmartspacerPluginRepository/main/plugins.json"

        private const val KEY_MONET_COLOR = "monet_color"
        private const val DEFAULT_MONET_COLOR = Integer.MAX_VALUE

        private const val KEY_INSTALL_TIME = "install_time"
        private const val DEFAULT_INSTALL_TIME = -1L

        private const val KEY_DONATE_PROMPT_ENABLED = "donate_prompt_enabled"
        private const val DEFAULT_DONATE_PROMPT_ENABLED = true

        private const val KEY_DONATE_PROMPT_DISMISSED_AT = "donate_prompt_dismissed_at"
        private const val DEFAULT_DONATE_PROMPT_DISMISSED_AT = -1L

        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        private const val DEFAULT_ANALYTICS_ENABLED = false

        private const val KEY_REQUIRES_DISPLAY_OVER_OTHER_APPS = "requires_display_over_other_apps"
        private const val DEFAULT_REQUIRES_DISPLAY_OVER_OTHER_APPS = false

        private const val KEY_NOTIFICATION_WIDGET_SERVICE_ENABLED = "notification_widget_service_enabled"
        private const val DEFAULT_NOTIFICATION_WIDGET_SERVICE_ENABLED = false

        private const val KEY_NOTIFICATION_WIDGET_TINT = "notification_widget_tint"
        private val DEFAULT_NOTIFICATION_WIDGET_TINT = TintColour.AUTOMATIC

        @Suppress("DEPRECATION")
        private fun SmartspacerSettingsRepositoryImpl.getDefaultExpandedBackground(): ExpandedBackground {
            return if(expandedBlurBackground.getSync()) {
                ExpandedBackground.BLUR
            }else{
                ExpandedBackground.SCRIM
            }
        }
    }

    override val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        SHARED_PREFS_NAME, Context.MODE_PRIVATE
    )

    override val hasSeenSetup = boolean(KEY_HAS_SEEN_SETUP, DEFAULT_HAS_SEEN_SETUP)
    override val userName = string(KEY_USER_NAME, DEFAULT_USER_NAME)
    override val isRestrictedModeDisabled = boolean(KEY_IS_RESTRICTED_MODE_DISABLED, DEFAULT_IS_RESTRICTED_MODE_DISABLED)
    override val enhancedMode = boolean(KEY_ENHANCED_MODE, DEFAULT_ENHANCED_MODE)
    override val hasUsedNativeMode = boolean(KEY_HAS_USED_NATIVE_MODE, DEFAULT_HAS_USED_NATIVE_MODE)
    override val nativeTargetCountLimit = enum(KEY_NATIVE_TARGET_COUNT, DEFAULT_NATIVE_TARGET_COUNT)
    override val nativeHideIncompatible = boolean(KEY_NATIVE_HIDE_INCOMPATIBLE, DEFAULT_NATIVE_HIDE_INCOMPATIBLE)
    override val nativeShowMediaSuggestions = boolean(KEY_NATIVE_SHOW_MEDIA_SUGGESTIONS, DEFAULT_NATIVE_SHOW_MEDIA_SUGGESTIONS)
    override val nativeUseSplitSmartspace = boolean(KEY_NATIVE_USE_SPLIT_SMARTSPACE, doesHaveSplitSmartspace())
    override val nativeImmediateStart = boolean(KEY_NATIVE_IMMEDIATE_START, DEFAULT_NATIVE_IMMEDIATE_START)
    override val hideSensitive = enum(KEY_HIDE_SENSITIVE, context.getSystemHideSensitive())
    override val expandedModeEnabled = boolean(KEY_EXPANDED_MODE_ENABLED, DEFAULT_EXPANDED_MODE_ENABLED)
    override val expandedShowHeader = boolean(KEY_EXPANDED_SHOW_HEADER, DEFAULT_EXPANDED_SHOW_HEADER)
    override val expandedShowSearchBox = boolean(KEY_EXPANDED_SHOW_SEARCH_BOX, DEFAULT_EXPANDED_SHOW_SEARCH_BOX)
    override val expandedSearchPackage = string(KEY_EXPANDED_SEARCH_PACKAGE, DEFAULT_EXPANDED_SEARCH_PACKAGE)
    override val expandedShowDoodle = boolean(KEY_EXPANDED_SHOW_DOODLE, DEFAULT_EXPANDED_SHOW_DOODLE)
    override val expandedOpenModeHome = enum(KEY_EXPANDED_OPEN_MODE_HOME, DEFAULT_EXPANDED_OPEN_MODE_HOME)
    override val expandedOpenModeLock = enum(KEY_EXPANDED_OPEN_MODE_LOCK, DEFAULT_EXPANDED_OPEN_MODE_LOCK)
    override val expandedCloseWhenLocked = boolean(KEY_EXPANDED_CLOSE_WHEN_LOCKED, DEFAULT_EXPANDED_CLOSE_WHEN_LOCKED)
    @Deprecated("No longer set in the settings")
    override val expandedBlurBackground = boolean(KEY_EXPANDED_BACKGROUND_BLUR, DEFAULT_EXPANDED_BACKGROUND_BLUR)
    override val expandedBackground = enum(KEY_EXPANDED_BACKGROUND, getDefaultExpandedBackground())
    override val expandedTintColour = enum(KEY_EXPANDED_TINT_COLOUR, DEFAULT_EXPANDED_TINT_COLOUR)
    override val expandedHasClickedAdd = boolean(KEY_EXPANDED_HAS_CLICKED_ADD, DEFAULT_EXPANDED_HAS_CLICKED_ADD)
    override val expandedWidgetUseGoogleSans = boolean(KEY_EXPANDED_WIDGETS_USE_GOOGLE_SANS, DEFAULT_EXPANDED_WIDGETS_USE_GOOGLE_SANS)
    override val expandedHideAddButton = enum(KEY_EXPANDED_HIDE_ADD_BUTTON, DEFAULT_EXPANDED_HIDE_ADD_BUTTON)
    override val expandedMultiColumnEnabled = boolean(KEY_EXPANDED_MULTI_COLUMN_ENABLED, DEFAULT_EXPANDED_MULTI_COLUMN_ENABLED)
    override val expandedXposedEnabled = boolean(KEY_EXPANDED_XPOSED_ENABLED, DEFAULT_EXPANDED_XPOSED_ENABLED)
    override val expandedComplicationsFirst = boolean(KEY_EXPANDED_COMPLICATIONS_FIRST, DEFAULT_EXPANDED_COMPLICATIONS_FIRST)
    override val expandedShowShadow = boolean(KEY_EXPANDED_SHOW_SHADOW, DEFAULT_EXPANDED_SHOW_SHADOW)
    override val oemSmartspaceEnabled = boolean(KEY_OEM_SMARTSPACE_ENABLED, DEFAULT_OEM_SMARTSPACE_ENABLED)
    override val oemHideIncompatible = boolean(KEY_OEM_HIDE_INCOMPATIBLE, DEFAULT_OEM_HIDE_INCOMPATIBLE)
    override val updateCheckEnabled = boolean(KEY_UPDATE_CHECK_ENABLED, DEFAULT_UPDATE_CHECK_ENABLED)
    override val pluginRepositoryEnabled = boolean(KEY_PLUGIN_REPOSITORY_ENABLED, DEFAULT_PLUGIN_REPOSITORY_ENABLED)
    override val pluginRepositoryUpdateCheckEnabled = boolean(KEY_PLUGIN_REPOSITORY_UPDATE_CHECK_ENABLED, DEFAULT_PLUGIN_REPOSITORY_UPDATE_CHECK_ENABLED)
    override val pluginRepositoryUrl = string(KEY_PLUGIN_REPOSITORY_URL, DEFAULT_PLUGIN_REPOSITORY_URL)
    override val monetColor = color(KEY_MONET_COLOR, DEFAULT_MONET_COLOR)
    override val installTime = long(KEY_INSTALL_TIME, DEFAULT_INSTALL_TIME)
    override val donatePromptEnabled = boolean(KEY_DONATE_PROMPT_ENABLED, DEFAULT_DONATE_PROMPT_ENABLED)
    override val donatePromptDismissedAt = long(KEY_DONATE_PROMPT_DISMISSED_AT, DEFAULT_DONATE_PROMPT_DISMISSED_AT)
    override val analyticsEnabled = boolean(KEY_ANALYTICS_ENABLED, DEFAULT_ANALYTICS_ENABLED)
    override val requiresDisplayOverOtherAppsPermission = boolean(KEY_REQUIRES_DISPLAY_OVER_OTHER_APPS, DEFAULT_REQUIRES_DISPLAY_OVER_OTHER_APPS)
    override val notificationWidgetServiceEnabled = boolean(KEY_NOTIFICATION_WIDGET_SERVICE_ENABLED, DEFAULT_NOTIFICATION_WIDGET_SERVICE_ENABLED)
    override val notificationWidgetTintColour = enum(KEY_NOTIFICATION_WIDGET_TINT, DEFAULT_NOTIFICATION_WIDGET_TINT)

    private fun getDefaultSharedPrefsVersion(): Int {
        /*
            Since this key was not previously set, we need to apply some logic to the default.

            If the value is already set (as is done on init in the upgrade check), it will use that.
            If the value is not set, but they have completed setup, they get a value of 1.
            If the value is not set and they have not completed setup, they are treated as new and
            it's set to the default.
         */
        return if(!hasSeenSetup.getSync()) {
            SHARED_PREFS_VERSION
        }else 1
    }

    private val sharedPrefsVersion = int(KEY_SHARED_PREFS_VERSION, getDefaultSharedPrefsVersion())

    override suspend fun setRestrictedModeKnownDisabledIfNeeded() {
        //Not sure if upgrading requires allowing unrestricted, so only setting on T+ for now
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        isRestrictedModeDisabled.set(true)
    }

    override suspend fun setInstallTimeIfNeeded() {
        if(!installTime.exists() || installTime.get() == DEFAULT_INSTALL_TIME) {
            installTime.set(System.currentTimeMillis())
        }
    }

    /**
     *  Use reflection to load all settings fields not marked with [IgnoreInBackup], which can be
     *  backed up and restored from
     */
    @VisibleForTesting
    fun getBackupFields(): List<SmartspacerSetting<*>> {
        return SmartspacerSettingsRepository::class.memberProperties.filter {
            it.findAnnotations<IgnoreInBackup>().isEmpty()
        }.map {
            it.get(this) as SmartspacerSetting<*>
        }
    }

    override suspend fun getBackup(): Map<String, String> {
        return getBackupFields().mapNotNull {
            Pair(it.key(), it.serialize() ?: return@mapNotNull null)
        }.toMap()
    }

    override suspend fun restoreBackup(settings: Map<String, String>) = withContext(Dispatchers.IO) {
        val properties = getBackupFields().associateBy { it.key() }
        settings.forEach {
            properties[it.key]?.deserialize(it.value)
        }
    }

    /**
     *  Android 14 introduces a split Smartspace on Pixels, which displays the first action
     *  (usually weather) persistently, above the content, with the date. For lock screen surfaces
     *  with this enabled, we need to split out the first action into its own target, which
     *  SystemUI will then separate out.
     *
     *  Some users may find this annoying, or may be using one of the clock styles where it causes
     *  issues, so it is possible to override this option to disable it.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun doesHaveSplitSmartspace(): Boolean {
        return isAtLeastU() &&
                SystemProperties.getBoolean("persist.sysui.ss.dw_decoupled", true)
    }

    /**
     *  Performs any necessary upgrades and then sets the current version
     */
    private fun upgradePrefsIfRequired() {
        val current = sharedPrefsVersion.getSync()
        if(current >= SHARED_PREFS_VERSION) return //Already up to date
        if(current < 2) {
            //1 -> 2: Expanded Smartspace for Xposed is enabled by default as was previously implied
            expandedXposedEnabled.setSync(true)
        }
        sharedPrefsVersion.setSync(SHARED_PREFS_VERSION)
    }

    init {
        upgradePrefsIfRequired()
    }

}