package com.kieronquinn.app.smartspacer.sdk.provider

import android.app.Activity
import android.content.ComponentName
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getProviderInfo

/**
 *  [SmartspacerRequirementProvider] is a [ContentProvider] that allows the addition of third party
 *  requirements to be added to Smartspacer Targets and Complications.
 *
 *  Implement [isRequirementMet] to return whether the requirement is met.
 */
abstract class SmartspacerRequirementProvider: BaseProvider() {

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET = "get_requirement"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_CONFIG = "get_requirement_config"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_ON_REMOVED = "on_removed"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_BACKUP = "backup"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_RESTORE = "restore"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val RESULT_KEY_REQUIREMENT_MET = "requirement_met"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_SMARTSPACER_ID = "smartspacer_id"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_BACKUP = "backup"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_SUCCESS = "success"

        private fun findAuthority(
            context: Context,
            provider: Class<out SmartspacerRequirementProvider>
        ): String {
            return context.packageManager.getProviderInfo(ComponentName(context, provider))
                .authority
        }

        /**
         *  Notify Smartspacer of a change to a given [provider], and that its state should be
         *  refreshed. Pass a [smartspacerId] to only refresh that provider, otherwise all providers
         *  of this type will be refreshed.
         */
        fun notifyChange(
            context: Context,
            provider: Class<out SmartspacerRequirementProvider>,
            smartspacerId: String? = null
        ) {
            val authority = findAuthority(context, provider)
            notifyChange(context, authority, smartspacerId)
        }

        /**
         *  Notify Smartspacer of a change to a given [authority], and that its state should be
         *  refreshed. Pass a [smartspacerId] to only refresh that provider, otherwise all providers
         *  of this type will be refreshed.
         */
        fun notifyChange(
            context: Context,
            authority: String,
            smartspacerId: String? = null
        ) {
            val uri = Uri.Builder().apply {
                scheme("content")
                authority(authority)
                if(smartspacerId != null){
                    appendPath(smartspacerId)
                }
            }.build()
            context.contentResolver?.notifyChange(uri, null, 0)
        }
    }

    /**
     *  Helper method to call [Companion.notifyChange], for the current provider. Use this to
     *  trigger a change from a local method, such as [restoreBackup]
     */
    protected fun notifyChange(smartspacerId: String? = null) {
        notifyChange(provideContext(), this::class.java, smartspacerId)
    }

    /**
     *  Returns whether the requirement is currently met for a given [smartspacerId]
     */
    abstract fun isRequirementMet(smartspacerId: String): Boolean

    /**
     *  Return the [Config] for this provider. This provides what to show in the UI for this
     *  requirement in Smartspacer, as well as settings for configuration
     *
     *  [smartspacerId]: The Smartspacer ID for the instance of this provider, or null if a generic
     *  config should be returned
     */
    abstract fun getConfig(smartspacerId: String?): Config

    /**
     *  Called when the provider is removed for a given [smartspacerId]. If you have not
     *  enabled [Config.allowAddingMoreThanOnce], this will not be called.
     *
     *  [smartspacerId]: The Smartspacer ID for the instance of this provider
     */
    open fun onProviderRemoved(smartspacerId: String) {
        //No-op by default
    }

    /**
     *  Serialize your Requirement to a [Backup]. Implement this to allow Smartspacer to backup your
     *  Requirement's data, which will be passed to [restoreBackup] during a restore.
     *
     *  This may be called multiple times per backup for different [smartspacerId]s.
     *
     *  **Note:** Even if you are unable to backup your Requirement, consider returning a [Backup]
     *  with [Backup.name] set. This will be displayed to the user during the restoration, as a hint
     *  of what the Requirement was configured to do, so they can reconfigure it.
     */
    open fun createBackup(smartspacerId: String): Backup {
        return Backup()
    }

    /**
     *  Deserialize a [backup], and store data which is being restored from the backup for a given
     *  [smartspacerId].
     *
     *  This may be called multiple times per restore for different [smartspacerId]s.
     *
     *  **Note:** If you returned a blank [backup] or one without [backup.data] set during
     *  [createBackup], this method will not be called.
     */
    open fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        return false
    }

    final override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        verifySecurity()
        return when(method){
            METHOD_GET -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                createRequirementMetBundle(smartspacerId)
            }
            METHOD_GET_CONFIG -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID)
                getConfig(smartspacerId).toBundle()
            }
            METHOD_ON_REMOVED -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                onProviderRemoved(smartspacerId)
                null
            }
            METHOD_BACKUP -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                bundleOf(EXTRA_BACKUP to createBackup(smartspacerId).toBundle())
            }
            METHOD_RESTORE -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val backup = extras.getBundle(EXTRA_BACKUP)?.let { Backup(it) } ?: return null
                bundleOf(EXTRA_SUCCESS to restoreBackup(smartspacerId, backup))
            }
            else -> null
        }
    }

    private fun createRequirementMetBundle(smartspacerId: String): Bundle {
        return bundleOf(
            RESULT_KEY_REQUIREMENT_MET to isRequirementMet(smartspacerId)
        )
    }

    data class Config(
        /**
         *  The label for this provider.
         */
        val label: CharSequence,
        /**
         *  A short description for this provider.
         */
        val description: CharSequence,
        /**
         *  The icon resource for this provider.
         */
        val icon: Icon,
        /**
         *  Whether this provider supports being added to a Smartspacer target or complication than
         *  once. Unlike this option in Targets and Actions, this does not prevent a requirement
         *  being added more than once across *all* targets and actions, and should only be used
         *  if it doesn't make sense for a Requirement to be added more than once, for example if
         *  it represents a hardware state.
         */
        val allowAddingMoreThanOnce: Boolean = true,
        /**
         *  The intent for configuring this requirement, if required. This can be opened from
         *  Smartspacer's settings, and is intended for editing settings such as which items to
         *  show.
         *
         *  It is not called during setup, see [setupActivity] for that.
         */
        val configActivity: Intent? = null,
        /**
         *  The intent which will be launched when this requirement is added. You **must** finish
         *  this activity with [Activity.RESULT_OK] for adding the requirement to succeed.
         */
        val setupActivity: Intent? = null,
        /**
         *  Whether this requirement is compatible with the device. Use
         *  [CompatibilityState.Compatible] when the device is compatible with the requirement, or
         *  [CompatibilityState.Incompatible] when the device is not. You should provide a reason
         *  for incompatibility, if set to `null` a generic message will be shown instead. Please
         *  note that this state is not dynamically updated, if your requirement is only compatible
         *  at certain times, for example if it requires a permission be granted first, you should
         *  mark it as compatible and return a requirement prompting the user to open the plugin
         *  app's settings and grant the permission.
         *
         *  A good example of a reason would be "This requirement requires Android 13"
         */
        val compatibilityState: CompatibilityState = CompatibilityState.Compatible
    ) {

        companion object {
            private const val KEY_LABEL = "label"
            private const val KEY_DESCRIPTION = "description"
            private const val KEY_ICON = "icon"
            private const val KEY_CONFIG_ACTIVITY = "config_activity"
            private const val KEY_SETUP_ACTIVITY = "setup_activity"
            private const val KEY_COMPATIBILITY_STATE = "compatibility_state"
            private const val KEY_ALLOW_ADDING_MORE_THAN_ONCE = "allow_adding_more_than_once"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getCharSequence(KEY_LABEL)!!,
            bundle.getCharSequence(KEY_DESCRIPTION)!!,
            bundle.getParcelableCompat(KEY_ICON, Icon::class.java)!!,
            bundle.getBoolean(KEY_ALLOW_ADDING_MORE_THAN_ONCE, true),
            bundle.getParcelableCompat<Intent>(KEY_CONFIG_ACTIVITY, Intent::class.java),
            bundle.getParcelableCompat<Intent>(KEY_SETUP_ACTIVITY, Intent::class.java),
            CompatibilityState.fromBundle(bundle.getBundle(
                KEY_COMPATIBILITY_STATE
            )!!)
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(): Bundle {
            return bundleOf(
                KEY_LABEL to label,
                KEY_DESCRIPTION to description,
                KEY_ICON to icon,
                KEY_ALLOW_ADDING_MORE_THAN_ONCE to allowAddingMoreThanOnce,
                KEY_COMPATIBILITY_STATE to compatibilityState.toBundle(),
                KEY_SETUP_ACTIVITY to setupActivity,
                KEY_CONFIG_ACTIVITY to configActivity
            )
        }

    }

}