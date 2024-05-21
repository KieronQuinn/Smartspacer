package com.kieronquinn.app.smartspacer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.assistant.oemsmartspace.shared.OEMSmartspaceSharedConstants
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.CardPriority
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.CardType
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.newBuilder
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.Smartspacer.Companion.PACKAGE_KEYGUARD
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.OemSmartspacerSession
import com.kieronquinn.app.smartspacer.repositories.AccessibilityRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.utils.extensions.Intent_FLAG_RECEIVER_FROM_SHELL
import com.kieronquinn.app.smartspacer.utils.extensions.displayOff
import com.kieronquinn.app.smartspacer.utils.extensions.isServiceRunning
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import com.kieronquinn.app.smartspacer.utils.extensions.runningApp
import com.kieronquinn.app.smartspacer.utils.extensions.sendBroadcast
import com.kieronquinn.app.smartspacer.utils.extensions.startForeground
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.ext.android.inject

/**
 *  Background service responsible for both OEM Smartspace (if enabled) and keeping the app running
 *  for calls to plugins.
 */
class SmartspacerBackgroundService: LifecycleService() {

    companion object {
        var isUsingEnhancedModeAppListener = false

        fun startServiceIfNeeded(context: Context) {
            if(isServiceRunning(context)) return
            val intent = Intent(context, SmartspacerBackgroundService::class.java)
            context.startService(intent)
        }

        private fun isServiceRunning(context: Context): Boolean {
            return context.isServiceRunning(SmartspacerBackgroundService::class.java)
        }
    }

    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()
    private val notifications by inject<NotificationRepository>()
    private val accessibilityRepository by inject<AccessibilityRepository>()
    private val settings by inject<SmartspacerSettingsRepository>()
    private val grantRepository by inject<GrantRepository>()
    private var previousVisiblePackage: String? = null
    private var sessions = emptyMap<String, OemSmartspacerSession>()

    private val runningApp = shizukuServiceRepository.suiService.filterNotNull().flatMapLatest {
        it.runningApp()
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val shizukuRrunningApp = shizukuServiceRepository.shizukuService.flatMapLatest {
        if(it != null) {
            isUsingEnhancedModeAppListener = true
            notifications.cancelNotification(NotificationId.ENABLE_ACCESSIBILITY)
            it.runningApp()
        }else{
            isUsingEnhancedModeAppListener = false
            flowOf(null)
        }
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val visiblePackage by lazy {
        combine(
            runningApp,
            lockscreenShowing(),
            displayOff()
        ) { visible, locked, off ->
            if(locked || off){
                PACKAGE_KEYGUARD
            }else{
                visible
            }
        }.filterNotNull()
    }

    private val oemPackages = grantRepository.grants.map {
        it?.filter { grant -> grant.oemSmartspace } ?: emptyList()
    }

    private val enabled = settings.oemSmartspaceEnabled.asFlow()

    override fun onCreate() {
        super.onCreate()
        startForeground(NotificationId.BACKGROUND_SERVICE, createNotification())
        setupOemSessions()
        setupOemVisibility()
        setupEnhancedModeAppListenerIfPossible()
    }

    override fun onDestroy() {
        notifications.cancelNotification(NotificationId.BACKGROUND_SERVICE)
        isUsingEnhancedModeAppListener = false
        sessions.values.forEach {
            it.onPause()
            it.onDestroy()
        }
        sessions = emptyMap()
        super.onDestroy()
    }

    private fun setupOemSessions() {
        combine(oemPackages, enabled) { packages, enabled ->
            sessions.values.forEach { session ->
                session.onPause()
                session.onDestroy()
            }
            if(packages.isEmpty() || !enabled){
                return@combine
            }
            if(!isOemCompatible()) {
                showErrorNotification()
                return@combine
            }
            sessions = packages.associate { grant ->
                val surface = if (grant.packageName == PACKAGE_KEYGUARD) {
                    UiSurface.LOCKSCREEN
                } else {
                    UiSurface.HOMESCREEN
                }
                Pair(grant.packageName, createSession(surface, grant.packageName))
            }
        }.launchIn(lifecycleScope)
    }

    private fun setupOemVisibility() = whenCreated {
        visiblePackage.debounce(250L).collect {
            sessions[previousVisiblePackage]?.onPause()
            sessions[it]?.let { session ->
                session.onResume()
                session.resendLastCards()
            }
            previousVisiblePackage = it
        }
    }

    private suspend fun isOemCompatible() = shizukuServiceRepository.runWithSuiService {
        it.isCompatible
    }.unwrap() ?: false

    private suspend fun onChange(sessionId: SmartspaceSessionId, cards: List<SmartspaceCard?>) {
        val primaryCard = cards.getOrNull(0) ?:
            createEmptyPrimaryTarget()
        val secondaryCard = cards.getOrNull(1) ?:
            createEmptySecondaryTarget()
        sendTarget(sessionId.id, primaryCard, secondaryCard)
    }

    private fun createSession(
        surface: UiSurface,
        packageName: String
    ): OemSmartspacerSession {
        return OemSmartspacerSession(
            this,
            SmartspaceConfig(1, surface, packageName),
            SmartspaceSessionId(packageName, Process.myUserHandle()),
            ::onChange
        )
    }

    @SuppressLint("WrongConstant")
    private suspend fun sendTarget(
        packageName: String,
        primary: SmartspaceCard,
        secondary: SmartspaceCard
    ) {
        shizukuServiceRepository.runWithSuiService {
            val update = SmartspaceUpdate.newBuilder()
                .addCard(primary)
                .addCard(secondary)
                .build()
                .toByteArray()
            val intent = Intent(OEMSmartspaceSharedConstants.UPDATE_ACTION).apply {
                addFlags(Intent_FLAG_RECEIVER_FROM_SHELL)
                putExtra(OEMSmartspaceSharedConstants.SMARTSPACE_CARD, update)
                `package` = packageName
            }
            it.sendBroadcast(this@SmartspacerBackgroundService, intent)
            //Some implementations don't update if not poked, so send a follow up broadcast
            val updateIntent = Intent(OEMSmartspaceSharedConstants.UPDATE_ACTION).apply {
                addFlags(Intent_FLAG_RECEIVER_FROM_SHELL)
                `package` = packageName
            }
            it.sendBroadcast(this@SmartspacerBackgroundService, updateIntent)
        }
    }

    private fun createEmptyPrimaryTarget(): SmartspaceCard {
        return newBuilder()
            .setShouldDiscard(true)
            .setCardPriority(CardPriority.PRIMARY)
            .setCardType(CardType.CALENDAR)
            .build()
    }

    /**
     *  Secondary targets can't be null with some implementations, but they can have no message or
     *  icon.
     */
    private fun createEmptySecondaryTarget(): SmartspaceCard {
        val text = FormattedText.newBuilder()
            .setText("")
            .build()
        val message = Message.newBuilder()
            .setTitle(text)
            .build()
        return newBuilder()
            .setShouldDiscard(false)
            .setCardPriority(CardPriority.SECONDARY)
            .setCardType(CardType.WEATHER)
            .setPreEvent(message)
            .setDuringEvent(message)
            .setPostEvent(message)
            .build()
    }

    private fun createNotification(): Notification {
        return notifications.showNotification(
            NotificationId.BACKGROUND_SERVICE,
            NotificationChannel.BACKGROUND_SERVICE
        ) {
            val notificationIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannel.BACKGROUND_SERVICE.id)
            }
            it.setContentTitle(getString(R.string.notification_title_background_service))
            it.setContentText(getString(R.string.notification_content_background_service))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.BACKGROUND_SERVICE.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_title_background_service))
        }
    }

    private fun showErrorNotification() {
        notifications.showNotification(
            NotificationId.BACKGROUND_SERVICE,
            NotificationChannel.OEM
        ) {
            it.setContentTitle(getString(R.string.notification_sui_title))
            it.setContentText(getString(R.string.notification_sui_content_oem_smartspace))
            it.setSmallIcon(R.drawable.ic_warning)
            it.setOngoing(false)
            it.setTicker(getString(R.string.notification_sui_title))
        }
    }

    private fun setupEnhancedModeAppListenerIfPossible() = whenCreated {
        shizukuRrunningApp.filterNotNull().collect {
            accessibilityRepository.setForegroundPackage(it)
        }
    }

}