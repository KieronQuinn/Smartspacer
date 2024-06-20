package com.kieronquinn.app.smartspacer.service

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Process
import android.service.notification.StatusBarNotification
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.model.media.MediaContainer
import com.kieronquinn.app.smartspacer.repositories.MediaRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.utils.extensions.isActiveCompat
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.utils.notification.LifecycleNotificationListenerService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.android.ext.android.inject

class SmartspacerNotificationListenerService: LifecycleNotificationListenerService() {

    companion object {

        private val COMPONENT = ComponentName(
            BuildConfig.APPLICATION_ID, SmartspacerNotificationListenerService::class.java.name
        )

        private var INSTANCE: SmartspacerNotificationListenerService? = null

        fun getAllNotificationChannels(packageName: String): List<NotificationChannel>? {
            return try {
                INSTANCE?.getNotificationChannels(packageName, Process.myUserHandle())
            }catch (e: SecurityException){
                return null
            }
        }

        fun getNotificationChannelGroups(packageName: String): List<NotificationChannelGroup>? {
            return try {
                INSTANCE?.getNotificationChannelGroups(packageName, Process.myUserHandle())
            }catch (e: SecurityException){
                return null
            }
        }
    }

    private val notificationRepository by inject<NotificationRepository>()
    private val mediaRepository by inject<MediaRepository>()
    private val settings by inject<SmartspacerSettingsRepository>()

    private val mediaSessionManager by lazy {
        getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    private val activeMediaSessions = callbackFlow {
        val sessions = {
            mediaSessionManager.getActiveSessions(COMPONENT)
        }
        val listener = MediaSessionManager.OnActiveSessionsChangedListener {
            trySend(sessions())
        }
        try {
            trySend(sessions())
            mediaSessionManager.addOnActiveSessionsChangedListener(listener, COMPONENT)
            awaitClose {
                mediaSessionManager.removeOnActiveSessionsChangedListener(listener)
            }
        }catch (e: SecurityException){
            //Weird system bug where listener starts despite not having permission
            awaitClose {  }
        }
    }

    private val activeMediaController = activeMediaSessions.flatMapLatest {
        it.filterNotNull().firstOrNull()?.isPlaying() ?: flowOf(null)
    }.map {
        it?.let { MediaContainer(it.packageName, it.metadata, it.sessionActivity) }
    }.distinctUntilChanged()

    private fun MediaController.isPlaying() = callbackFlow {
        val emitIfActive = {
            if(playbackState?.isActiveCompat() == true){
                trySend(this@isPlaying)
            }else{
                trySend(null)
            }
        }
        val callback = object: MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                emitIfActive()
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                emitIfActive()
            }
        }
        emitIfActive()
        registerCallback(callback)
        awaitClose {
            unregisterCallback(callback)
        }
    }.debounce(250L)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationPosted(sbn, rankingMap)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationRemoved(sbn, rankingMap)
        updateNotifications()
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        setupDismissBus()
        whenCreated {
            settings.setRestrictedModeKnownDisabledIfNeeded()
        }
    }

    override fun onDestroy() {
        INSTANCE = null
        mediaRepository.setMediaContainer(null)
        super.onDestroy()
    }

    private fun updateNotifications() = whenCreated {
        notificationRepository.updateNotifications(getActiveNotificationsSafely().toList())
    }

    private fun setupDismissBus() = whenCreated {
        notificationRepository.dismissNotificationBus.collect {
            cancelNotification(it.key)
        }
    }

    private fun getActiveNotificationsSafely(): Array<StatusBarNotification> {
        return try {
            activeNotifications
        }catch (e: Throwable) {
            //Weird system issue
            emptyArray()
        }
    }

    private fun setupMediaControllerListener() = whenCreated {
        activeMediaController.collect {
            mediaRepository.setMediaContainer(it)
        }
    }

    init {
        setupMediaControllerListener()
    }

}