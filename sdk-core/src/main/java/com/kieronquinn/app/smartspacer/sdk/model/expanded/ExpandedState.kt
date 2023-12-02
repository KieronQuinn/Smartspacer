package com.kieronquinn.app.smartspacer.sdk.model.expanded

import android.app.PendingIntent
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts.Shortcut
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import kotlinx.parcelize.Parcelize
import android.widget.RemoteViews as SystemRemoteViews

/**
 *  Contains an expanded UI for Smartspacer to display in expanded Smartspace. This can show:
 *
 *  - An arbitrary RemoteViews view, to display whatever content you want for the given Target.
 *  - An [AppWidgetProviderInfo] to display a given widget, which will be loaded into Smartspacer
 *  - A list of [Shortcuts], with basic [Shortcut] info. These can be entirely custom.
 *  - Whether to display App Shortcuts, and which packages to load them from - requires enhanced
 *  mode.
 */
@Parcelize
data class ExpandedState(
    val remoteViews: RemoteViews? = null,
    val widget: Widget? = null,
    val shortcuts: Shortcuts? = null,
    val appShortcuts: AppShortcuts? = null
): Parcelable {

    companion object {
        private const val KEY_REMOTE_VIEWS = "remote_views"
        private const val KEY_WIDGET = "widget"
        private const val KEY_SHORTCUTS = "shortcuts"
        private const val KEY_APP_SHORTCUTS = "app_shortcuts"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getBundle(KEY_REMOTE_VIEWS)?.let { RemoteViews(it) },
        bundle.getBundle(KEY_WIDGET)?.let { Widget(it) },
        bundle.getBundle(KEY_SHORTCUTS)?.let { Shortcuts(it) },
        bundle.getBundle(KEY_APP_SHORTCUTS)?.let { AppShortcuts(it) }
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBundle(): Bundle {
        return bundleOf(
            KEY_REMOTE_VIEWS to remoteViews?.toBundle(),
            KEY_WIDGET to widget?.toBundle(),
            KEY_SHORTCUTS to shortcuts?.toBundle(),
            KEY_APP_SHORTCUTS to appShortcuts?.toBundle()
        )
    }

    /**
     *  Show an arbitrary RemoteViews view below the Target. This can be any RemoteViews content,
     *  much like a Widget.
     *
     *  Set the [unlocked] and [locked] fields to show different views when the expanded state is
     *  displaying while locked, setting only [view] will apply to both. To display only when
     *  unlocked, set [unlocked] and leave [locked] and [view] as null (the same can be done for
     *  [locked] to set only the locked RemoteViews.
     */
    @Parcelize
    data class RemoteViews(
        val view: SystemRemoteViews? = null,
        val unlocked: SystemRemoteViews? = view,
        val locked: SystemRemoteViews? = view
    ): Parcelable {

        companion object {
            private const val KEY_VIEW = "view"
            private const val KEY_UNLOCKED = "unlocked"
            private const val KEY_LOCKED = "locked"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getParcelableCompat(KEY_VIEW, SystemRemoteViews::class.java),
            bundle.getParcelableCompat(KEY_UNLOCKED, SystemRemoteViews::class.java),
            bundle.getParcelableCompat(KEY_LOCKED, SystemRemoteViews::class.java),
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(): Bundle {
            return bundleOf(
                KEY_VIEW to view,
                KEY_UNLOCKED to unlocked,
                KEY_LOCKED to locked
            )
        }

    }

    /**
     *  Shows a widget, passed as [info].
     *
     *  Specify an [id] if you wish to have multiple instances of a widget (for example two weather
     *  locations), or wish to use width/height.
     *
     *  **Important**: If you wish to specify a [width] and [height] for the widget (rather than
     *  have Smartspacer use the default size for this widget), you **must** specify an [id]. This
     *  ID should stay the same in subsequent calls of this size, and should not be re-used for
     *  widgets of a different size - unless you want to resize the widget, in which case using the
     *  same ID will update the size of this instance.
     *
     *  You can also prevent the widget being shown by setting [showWhenLocked] to false.
     */
    @Parcelize
    data class Widget(
        val info: AppWidgetProviderInfo,
        val id: String? = null,
        val showWhenLocked: Boolean = true,
        val height: Int = 0,
        val width: Int = 0
    ): Parcelable {

        init {
            if((height != 0 || width != 0) && id.isNullOrBlank()){
                throw IllegalArgumentException("You must specify an ID to use Widget width/height")
            }
        }

        companion object {
            private const val KEY_INFO = "info"
            private const val KEY_ID = "id"
            private const val KEY_SHOW_WHEN_LOCKED = "show_when_locked"
            private const val KEY_HEIGHT = "height"
            private const val KEY_WIDTH = "width"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getParcelableCompat(KEY_INFO, AppWidgetProviderInfo::class.java)!!,
            bundle.getString(KEY_ID),
            bundle.getBoolean(KEY_SHOW_WHEN_LOCKED),
            bundle.getInt(KEY_HEIGHT),
            bundle.getInt(KEY_WIDTH)
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(): Bundle {
            return bundleOf(
                KEY_INFO to info,
                KEY_ID to id,
                KEY_SHOW_WHEN_LOCKED to showWhenLocked,
                KEY_HEIGHT to height,
                KEY_WIDTH to width
            )
        }

    }
    /**
     *  Shows up to 10 [Shortcut]s, pass in [shortcuts] list.
     */
    @Parcelize
    data class Shortcuts(val shortcuts: List<Shortcut>): Parcelable {

        companion object {
            private const val KEY_SHORTCUTS = "shortcuts"
        }

        /**
         *  An arbitrary shortcut to either an [intent] or [pendingIntent] ([PendingIntent]
         *  prioritised). Specify an [icon] and [label], and whether to [showWhenLocked] (optional)
         */
        @Parcelize
        data class Shortcut(
            val label: CharSequence?,
            val icon: Icon?,
            val intent: Intent? = null,
            val pendingIntent: PendingIntent? = null,
            val showWhenLocked: Boolean = true
        ): BaseShortcut(ItemType.SHORTCUT), Parcelable {

            companion object {
                private const val KEY_LABEL = "label"
                private const val KEY_ICON = "icon"
                private const val KEY_INTENT = "intent"
                private const val KEY_PENDING_INTENT = "pending_intent"
                private const val KEY_SHOW_WHEN_LOCKED = "show_when_locked"
            }

            @RestrictTo(RestrictTo.Scope.LIBRARY)
            constructor(bundle: Bundle): this(
                bundle.getCharSequence(KEY_LABEL),
                bundle.getBundle(KEY_ICON)?.let { Icon(it) },
                bundle.getParcelableCompat(KEY_INTENT, Intent::class.java),
                bundle.getParcelableCompat(KEY_PENDING_INTENT, PendingIntent::class.java),
                bundle.getBoolean(KEY_SHOW_WHEN_LOCKED)
            )

            @RestrictTo(RestrictTo.Scope.LIBRARY)
            fun toBundle(): Bundle {
                return bundleOf(
                    KEY_LABEL to label,
                    KEY_ICON to icon?.toBundle(),
                    KEY_INTENT to intent,
                    KEY_PENDING_INTENT to pendingIntent,
                    KEY_SHOW_WHEN_LOCKED to showWhenLocked
                )
            }

            override fun equals(other: Any?): Boolean {
                if(other !is Shortcut) return false
                if(label != other.label) return false
                //Icon & intents cannot be checked
                if(showWhenLocked != other.showWhenLocked) return false
                return true
            }

        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getParcelableArrayListCompat(KEY_SHORTCUTS, Bundle::class.java)?.map {
                Shortcut(it)
            } ?: emptyList()
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(): Bundle {
            return bundleOf(
                KEY_SHORTCUTS to shortcuts.map { it.toBundle() }
            )
        }

    }

    /**
     *  Shows App Shortcuts from a given set of [packageNames]. This requires Shizuku or Sui, so
     *  will only be shown when Enhanced Mode is enabled.
     *
     *  [appShortcutCount] specifies the maximum number of App Shortcuts to show from each package,
     *  this is capped to 10 shortcuts total across all packages in Smartspacer; if multiple
     *  packages are specified, one App Shortcut will be sequentially taken from each until the
     *  limit is reached or none are left, whichever happens first.
     *
     *  Set [showWhenLocked] to false to not display App Shortcuts in this expanded state when
     *  locked.
     */
    @Parcelize
    data class AppShortcuts(
        val packageNames: Set<String>,
        val appShortcutCount: Int = 5,
        val showWhenLocked: Boolean = true
    ): Parcelable {

        companion object {
            private const val KEY_PACKAGE_NAMES = "package_names"
            private const val KEY_APP_SHORTCUT_COUNT = "app_shortcut_count"
            private const val KEY_SHOW_WHEN_LOCKED = "show_when_locked"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getStringArrayList(KEY_PACKAGE_NAMES)!!.toSet(),
            bundle.getInt(KEY_APP_SHORTCUT_COUNT),
            bundle.getBoolean(KEY_SHOW_WHEN_LOCKED)
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(): Bundle {
            return bundleOf(
                KEY_PACKAGE_NAMES to ArrayList(packageNames),
                KEY_APP_SHORTCUT_COUNT to appShortcutCount,
                KEY_SHOW_WHEN_LOCKED to showWhenLocked
            )
        }

    }

    abstract class BaseShortcut(val itemType: ItemType) {
        enum class ItemType {
            APP_SHORTCUT, SHORTCUT
        }
    }

}


