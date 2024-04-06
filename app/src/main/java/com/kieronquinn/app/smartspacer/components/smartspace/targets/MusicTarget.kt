package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.app.Notification.EXTRA_MEDIA_SESSION
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaMetadata.METADATA_KEY_ALBUM_ART
import android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI
import android.media.MediaMetadata.METADATA_KEY_ART
import android.media.MediaMetadata.METADATA_KEY_ARTIST
import android.media.MediaMetadata.METADATA_KEY_ART_URI
import android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE
import android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE
import android.media.MediaMetadata.METADATA_KEY_TITLE
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.model.media.MediaContainer
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.MediaRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.DoorbellState
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import com.kieronquinn.app.smartspacer.ui.activities.permission.notification.NotificationPermissionActivity
import com.kieronquinn.app.smartspacer.utils.extensions.getDisplayPortraitWidth
import com.kieronquinn.app.smartspacer.utils.extensions.getDisplayWidth
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetColumnWidth
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetRowHeight
import com.kieronquinn.app.smartspacer.utils.extensions.isFourByOne
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class MusicTarget: SmartspacerTargetProvider() {

    companion object {
        private const val TARGET_ID_PREFIX = "music_"

        /**
         *  Optional widget mapping for music apps. If no widget is specified in this map, a
         *  suitable 4*1 widget will be used instead where available.
         */
        private val WIDGET_MAPPING = mapOf(
            "com.spotify.music" to "com.spotify.proactiveplatforms.npvwidget.NpvWidgetProvider"
        )

        /**
         *  Packages whose notifications cannot be launched and should always fallback to a regular
         *  launch
         */
        private val SESSION_PACKAGE_DENYLIST = setOf(
            "com.google.android.youtube"
        )
    }

    private val mediaRepository by inject<MediaRepository>()
    private val notificationRepository by inject<NotificationRepository>()
    private val dataRepository by inject<DataRepository>()
    private val widgetRepository by inject<WidgetRepository>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val container = mediaRepository.mediaController.value ?: return emptyList()
        return listOfNotNull(container.loadTarget(smartspacerId))
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.target_music_title),
            description = resources.getString(R.string.target_music_description),
            icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_music),
            setupActivity = Intent(provideContext(), NotificationPermissionActivity::class.java),
            configActivity = ConfigurationActivity.createIntent(
                provideContext(), NavGraphMapping.TARGET_MUSIC
            )
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        val packageName = targetId.removePrefix(TARGET_ID_PREFIX)
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.MUSIC,
            ::onDataChanged
        ){
            val data = it ?: TargetData()
            data.copy(hiddenPackages = data.hiddenPackages.plus(packageName))
        }
        return true
    }

    override fun createBackup(smartspacerId: String): Backup {
        val settings = dataRepository.getTargetData(smartspacerId, TargetData::class.java)
            ?: return Backup()
        val gson = get<Gson>()
        return Backup(gson.toJson(settings))
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val gson = get<Gson>()
        val settings = try {
            gson.fromJson(backup.data ?: return false, TargetData::class.java)
        }catch (e: Exception){
            return false
        }
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.MUSIC,
            ::restoreNotifyChange
        ){
            TargetData(
                settings.showAlbumArt,
                settings.useDoorbell,
                settings.hiddenPackages
            )
        }
        return false // We still want to fire config to force notification permission setup
    }

    private fun restoreNotifyChange(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun onDataChanged(context: Context, smartspacerId: String) {
        notifyChange(context, MusicTarget::class.java, smartspacerId)
    }

    override fun onProviderRemoved(smartspacerId: String) {
        dataRepository.deleteTargetData(smartspacerId)
    }

    private fun MediaContainer.loadTarget(smartspacerId: String): SmartspaceTarget? {
        val settings = dataRepository.getTargetData(smartspacerId, TargetData::class.java)
            ?: TargetData()
        if(settings.hiddenPackages.contains(packageName)) return null
        val art = if(settings.showAlbumArt){
            metadata?.getAlbumArt()
        }else null
        return when {
            art != null && settings.useDoorbell -> {
                loadTargetWithDoorbellArt(art)
            }
            art != null -> {
                loadTargetWithArt(art)
            }
            else -> {
                loadTargetWithoutArt()
            }
        }.also {
            it?.expandedState = getExpandedState(smartspacerId)
        }
    }

    private fun MediaContainer.loadTargetWithoutArt(): SmartspaceTarget? {
        val metadata = metadata ?: return null
        val title = metadata.getTitle() ?: return null
        val subtitle = metadata.getSubtitle(packageName)
        return TargetTemplate.Basic(
            id = "$TARGET_ID_PREFIX$packageName",
            componentName = ComponentName(provideContext(), MusicTarget::class.java),
            icon = Icon(getIcon()),
            title = Text(title),
            subtitle = Text(subtitle),
            onClick = getClickAction()
        ).create()
    }

    private fun MediaContainer.loadTargetWithArt(albumArt: Bitmap): SmartspaceTarget? {
        val metadata = metadata ?: return null
        val title = metadata.getTitle() ?: return null
        val subtitle = metadata.getSubtitle(packageName)
        return TargetTemplate.Image(
            provideContext(),
            id = "music_$packageName",
            componentName = ComponentName(provideContext(), MusicTarget::class.java),
            icon = Icon(getIcon()),
            image = Icon(AndroidIcon.createWithBitmap(albumArt), shouldTint = false),
            title = Text(title),
            subtitle = Text(subtitle),
            onClick = getClickAction()
        ).create()
    }

    private fun MediaContainer.loadTargetWithDoorbellArt(albumArt: Bitmap): SmartspaceTarget? {
        val metadata = metadata ?: return null
        val title = metadata.getTitle() ?: return null
        val subtitle = metadata.getSubtitle(packageName)
        val state = DoorbellState.ImageBitmap(albumArt)
        return TargetTemplate.Doorbell(
            id = "music_$packageName",
            componentName = ComponentName(provideContext(), MusicTarget::class.java),
            icon = Icon(getIcon()),
            doorbellState = state,
            title = Text(title),
            subtitle = Text(subtitle),
            onClick = getClickAction()
        ).create()
    }

    /**
     *  Loads album art for this Target, if available. If a Bitmap has been provided, use that,
     *  otherwise try to load the art from a URL, or if not available fall back to no art.
     */
    private fun MediaMetadata.getAlbumArt(): Bitmap? {
        return when {
            containsKey(METADATA_KEY_ART) -> {
                getBitmap(METADATA_KEY_ART)
            }
            containsKey(METADATA_KEY_ART_URI) -> {
                val uri = Uri.parse(getString(METADATA_KEY_ART_URI))
                val inputStream = provideContext().contentResolver.openInputStream(uri)
                    ?: return null
                BitmapFactory.decodeStream(inputStream).also {
                    inputStream.close()
                }
            }
            containsKey(METADATA_KEY_ALBUM_ART) -> {
                getBitmap(METADATA_KEY_ALBUM_ART)
            }
            containsKey(METADATA_KEY_ALBUM_ART_URI) -> {
                val uri = Uri.parse(getString(METADATA_KEY_ALBUM_ART_URI))
                val inputStream = provideContext().contentResolver.openInputStream(uri)
                    ?: return null
                BitmapFactory.decodeStream(inputStream).also {
                    inputStream.close()
                }
            }
            else -> null
        }
    }

    /**
     *  Load a suitable title for the Target - either the specified display title, or regular title
     */
    private fun MediaMetadata.getTitle(): String? {
        return getString(METADATA_KEY_DISPLAY_TITLE) ?: getString(METADATA_KEY_TITLE)
    }

    /**
     *  Load a suitable subtitle for the Target - either the artist, subtitle, or fallback
     */
    private fun MediaMetadata.getSubtitle(packageName: String): String {
        return getString(METADATA_KEY_ARTIST) ?: getString(METADATA_KEY_DISPLAY_SUBTITLE)
            ?: provideContext().getFallbackSubtitle(packageName)
    }

    /**
     *  Get the fallback subtitle for this Target, if an artist or subtitle have not been specified.
     *  This is either the app's name or, at worst case, the target name
     */
    private fun Context.getFallbackSubtitle(packageName: String): String {
        return provideContext().packageManager.getPackageLabel(packageName)?.toString()
            ?: resources.getString(R.string.target_music_title)
    }

    /**
     *  Try to find an icon for this Target, in the order of:
     *  Notification with player > Notification without player > Fallback icon.
     *  This resolves the issue in Android 12+ where the stock music player doesn't always show an
     *  icon.
     */
    private fun MediaContainer.getIcon(): AndroidIcon {
        val notificationIcon = notificationRepository.activeNotifications.value.firstOrNull {
            it.packageName == packageName && it.notification.extras.containsKey(EXTRA_MEDIA_SESSION)
        }?.notification?.smallIcon ?: notificationRepository.activeNotifications.value.firstOrNull {
            it.packageName == packageName
        }?.notification?.smallIcon
        return notificationIcon ?: AndroidIcon.createWithResource(
            provideContext(), R.drawable.ic_target_music
        )
    }

    /**
     *  Opens the provided session activity, or falls back to the regular app launch for the package
     */
    private fun MediaContainer.getClickAction(): TapAction? {
        if(sessionActivity != null && !SESSION_PACKAGE_DENYLIST.contains(packageName)){
            return TapAction(pendingIntent = sessionActivity)
        }
        val launchIntent = provideContext().packageManager.getLaunchIntentForPackage(packageName)
            ?: return null
        return TapAction(intent = launchIntent)
    }

    /**
     *  Finds a suitable app widget (4x1) and shows it and any app shortcuts for this app in the
     *  expanded state.
     */
    private fun MediaContainer.getExpandedState(id: String): ExpandedState {
        return ExpandedState(
            appShortcuts = ExpandedState.AppShortcuts(
                setOf(packageName)
            ),
            widget = findSuitableWidget()?.let {
                ExpandedState.Widget(
                    info = it,
                    id = id,
                    width = provideContext().getDisplayPortraitWidth()
                )
            }
        )
    }

    private fun MediaContainer.findSuitableWidget(): AppWidgetProviderInfo? {
        //Prefer custom mapping, if available
        WIDGET_MAPPING[packageName]?.let { clazz ->
            widgetRepository.getProviders().firstOrNull {
                it.provider.packageName == packageName && it.provider.className == clazz
            }?.let { return it }
        }
        val availableWidth = provideContext().getDisplayWidth()
        val columnWidth = provideContext().getWidgetColumnWidth(availableWidth)
        val rowHeight = provideContext().getWidgetRowHeight(availableWidth)
        return widgetRepository.getProviders().firstOrNull {
            it.provider.packageName == packageName && it.isFourByOne(columnWidth, rowHeight)
        }
    }

    data class TargetData(
        @SerializedName("show_album_art")
        val showAlbumArt: Boolean = true,
        @SerializedName("use_doorbell")
        val useDoorbell: Boolean = false,
        @SerializedName("hidden_packages")
        val hiddenPackages: Set<String> = emptySet()
    )

}