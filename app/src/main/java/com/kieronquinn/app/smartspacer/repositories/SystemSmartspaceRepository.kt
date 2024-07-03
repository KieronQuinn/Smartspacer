package com.kieronquinn.app.smartspacer.repositories

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.smartspace.SmartspaceConfig
import android.content.Context
import android.content.Intent
import android.content.pm.ParceledListSlice
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.kieronquinn.app.smartspacer.ISmartspaceOnTargetsAvailableListener
import com.kieronquinn.app.smartspacer.ISmartspacerShizukuService
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.Smartspacer
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.receivers.NativeDismissReceiver
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport.Companion.isNativeModeAvailable
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository.ShizukuServiceResponse
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.service.SmartspacerSmartspaceService
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.screens.native.NativeModeFragment
import com.kieronquinn.app.smartspacer.ui.screens.native.reconnect.NativeReconnectFragment
import com.kieronquinn.app.smartspacer.utils.extensions.PendingIntent_MUTABLE_FLAGS
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultSmartspaceComponent
import com.kieronquinn.app.smartspacer.utils.extensions.getIdentifier
import com.kieronquinn.app.smartspacer.utils.extensions.toSmartspaceTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import android.app.smartspace.SmartspaceTarget as SystemSmartspaceTarget


/**
 *  Repository responsible for interacting with the system Smartspace Service, before Smartspacer
 *  sets its own. At the time that the session is created by this, it will be connected to the
 *  system service, if available.
 */
interface SystemSmartspaceRepository {

    /**
     *  Sets the Smartspacer service as the native system Smartspace service
     */
    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun setService(): Boolean

    /**
     *  Un-sets the Smartspacer service. Setting [onlyIfAvailable] to true will not start Shizuku
     *  and only use an existing connection (for safe mode)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun resetService(onlyIfAvailable: Boolean = false, killSystemUi: Boolean = true): Boolean

    /**
     *  An (estimated) current state of the native service. Set to true in service's onCreate,
     *  and reset if [resetService] is called.
     */
    val serviceRunning: StateFlow<Boolean>

    /**
     *  Regular home targets from plugins
     */
    val homeTargets: StateFlow<List<SmartspaceTarget>>

    /**
     *  Regular lock targets from plugins
     */
    val lockTargets: StateFlow<List<SmartspaceTarget>>

    /**
     *  Media targets, only from the system
     */
    val mediaTargets: StateFlow<List<SmartspaceTarget>>

    /**
     *  Glanceable Hub targets, only from the system
     */
    val hubTargets: StateFlow<List<SmartspaceTarget>>

    /**
     *  Dismisses the given Target **locally**. We cannot persist dismissals into the default
     *  service, as the actual inbound connection is severed.
     */
    fun dismissDefaultTarget(targetId: String)

    /**
     *  Called from native service's onCreate
     */
    fun notifyServiceRunning()

    /**
     *  Shows a notification reminding the user to re-enable the native service if required
     */
    fun showNativeStartReminderIfNeeded()

    /**
     *  Triggered when Android System Intelligence stops (by a crash, force stop or update),
     *  displays a notification to the user to disable and re-enable Native Smartspace to allow a
     *  reconnection.
     */
    fun onAsiStopped()

    /**
     *  Triggered when the System Smartspace Service detects a self-connection, and thus has
     *  disabled itself, displays a notification to the user to disable and re-enable Native
     *  Smartspace to fix and reconnect.
     */
    fun onFeedbackLoopDetected()

}

@SuppressLint("newApi")
class SystemSmartspaceRepositoryImpl(
    private val context: Context,
    private val shizuku: ShizukuServiceRepository,
    private val settings: SmartspacerSettingsRepository,
    private val compatibility: CompatibilityRepository,
    private val notifications: NotificationRepository,
    private val scope: CoroutineScope = MainScope()
): SystemSmartspaceRepository, KoinComponent {

    companion object {
        //Don't show the notifications more than once per 60s
        private const val LAST_SHOWED_NOTIFICATION_RATE_LIMIT = 60_000L
    }

    @VisibleForTesting
    val _homeTargets = MutableStateFlow<List<SmartspaceTarget>>(emptyList())

    @VisibleForTesting
    val _lockTargets = MutableStateFlow<List<SmartspaceTarget>>(emptyList())

    @VisibleForTesting
    val _mediaTargets = MutableStateFlow<List<SmartspaceTarget>>(emptyList())

    @VisibleForTesting
    val _hubTargets = MutableStateFlow<List<SmartspaceTarget>>(emptyList())

    private val user = Process.myUserHandle().getIdentifier()
    private var lastShowedNativeReminderAt = 0L
    private var lastShowedReconnectAt = 0L

    override val serviceRunning = MutableStateFlow(false)

    override val homeTargets = combine(
        _homeTargets.asStateFlow(),
        settings.enhancedMode.asFlow()
    ) { targets, enabled ->
        if(enabled) targets else emptyList()
    }.stateIn(scope, SharingStarted.Eagerly, _homeTargets.value)

    override val lockTargets = combine(
        _lockTargets.asStateFlow(),
        settings.enhancedMode.asFlow()
    ) { targets, enabled ->
        if(enabled) targets else emptyList()
    }.stateIn(scope, SharingStarted.Eagerly, _lockTargets.value)

    override val mediaTargets = combine(
        _mediaTargets.asStateFlow(),
        settings.nativeShowMediaSuggestions.asFlow(),
        settings.enhancedMode.asFlow()
    ) { targets, native, enabled ->
        if(native && enabled) targets else emptyList()
    }.stateIn(scope, SharingStarted.Eagerly, _mediaTargets.value)

    override val hubTargets = combine(
        _hubTargets.asStateFlow(),
        settings.enhancedMode.asFlow()
    ) { targets, enabled ->
        if(enabled) targets else emptyList()
    }.stateIn(scope, SharingStarted.Eagerly, _hubTargets.value)

    /**
     *  Whether the system targets should be monitored, which requires Shizuku ready and enhanced
     *  mode be on. Since sessions survive component changes, this should only emit once.
     */
    private val shouldBeEnabled = combine(
        settings.enhancedMode.asFlow(),
        shizuku.isReady
    ) { enabled, ready ->
        //Safety check - never enabled on < 12
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@combine false
        enabled && ready
    }.filter { it }.take(1)

    private fun setupService() = scope.launch {
        shouldBeEnabled.collect {
            initService()
        }
    }

    private suspend fun initService() {
        shizuku.runWithService {
            it.setupSession(UiSurface.HOMESCREEN) { targets ->
                _homeTargets.emit(targets)
            }
            it.setupSession(UiSurface.LOCKSCREEN) { targets ->
                _lockTargets.emit(targets)
            }
            it.setupSession(UiSurface.MEDIA_DATA_MANAGER) { targets ->
                _mediaTargets.emit(targets)
            }
            it.setupSession(UiSurface.GLANCEABLE_HUB) { targets ->
                _hubTargets.emit(targets)
            }
            true
        }
    }

    private fun ISmartspacerShizukuService.setupSession(
        surface: UiSurface, onTargetsAvailable: suspend (targets: List<SmartspaceTarget>) -> Unit
    ) {
        val smartspaceConfig = SmartspaceConfig.Builder(context, surface.surface)
            .setSmartspaceTargetCount(5)
            .build()
        val session = createSmartspaceSession(smartspaceConfig)
        val callback = createCallback(onTargetsAvailable)
        session?.addOnTargetsAvailableListener(callback)
        session?.requestSmartspaceUpdate()
    }

    @Suppress("UNCHECKED_CAST")
    private fun createCallback(
        onTargetsAvailable: suspend (targets: List<SmartspaceTarget>) -> Unit
    ): ISmartspaceOnTargetsAvailableListener.Stub {
        return object: ISmartspaceOnTargetsAvailableListener.Stub() {
            override fun onTargetsAvailable(targets: ParceledListSlice<*>?) {
                val availableTargets = (targets as ParceledListSlice<SystemSmartspaceTarget>).list
                scope.launch(Dispatchers.IO) {
                    val converted = availableTargets.map { it.toSmartspaceTarget() }
                    onTargetsAvailable(converted)
                }
            }
        }
    }

    override fun dismissDefaultTarget(targetId: String) {
        scope.launch {
            homeTargets.dismissTarget(targetId, _homeTargets)
            lockTargets.dismissTarget(targetId, _lockTargets)
        }
    }

    private suspend fun StateFlow<List<SmartspaceTarget>>.dismissTarget(
        targetId: String,
        emitTo: MutableStateFlow<List<SmartspaceTarget>>
    ) {
        val defaultTargets = value.toMutableList()
        val target = defaultTargets.firstOrNull { it.smartspaceTargetId == targetId } ?: return
        defaultTargets.remove(target)
        emitTo.emit(defaultTargets)
    }

    init {
        setupService()
    }

    override suspend fun setService(): Boolean {
        return withContext(Dispatchers.IO) {
            shizuku.runWithService {
                val killPackages = getLaunchersToKill()
                it.setSmartspaceService(
                    SmartspacerSmartspaceService.COMPONENT, user, true, killPackages
                )
            } is ShizukuServiceResponse.Success
        }
    }

    override suspend fun resetService(onlyIfAvailable: Boolean, killSystemUi: Boolean): Boolean {
        return withContext(Dispatchers.IO){
            val default = context.getDefaultSmartspaceComponent()
            val killPackages = if(killSystemUi) {
                getLaunchersToKill()
            }else emptyList()
            val block = { service: ISmartspacerShizukuService ->
                if(default != null){
                    service.setSmartspaceService(default, user, killSystemUi, killPackages)
                }else{
                    service.clearSmartspaceService(user, killSystemUi, killPackages)
                }
            }
            val result = if(onlyIfAvailable){
                shizuku.runWithServiceIfAvailable(block)
            }else {
                shizuku.runWithService(block)
            } is ShizukuServiceResponse.Success
            serviceRunning.emit(false)
            setupService()
            result
        }
    }

    private suspend fun getLaunchersToKill(): List<String> {
        return compatibility.getCompatibilityReports().map { compatibility ->
            compatibility.packageName
        }.filter { packageName ->
            packageName != Smartspacer.PACKAGE_KEYGUARD
        }
    }

    override fun notifyServiceRunning() {
        scope.launch {
            serviceRunning.emit(true)
        }
    }

    override fun showNativeStartReminderIfNeeded() {
        scope.launch {
            val now = System.currentTimeMillis()
            //Rate limit showing notifications / restarting
            if(now - lastShowedNativeReminderAt <= LAST_SHOWED_NOTIFICATION_RATE_LIMIT) return@launch
            lastShowedNativeReminderAt = now
            //Check Enhanced Mode is on, it's still compatible, and native mode has previously started
            if(settings.enhancedMode.get() && settings.hasUsedNativeMode.get() &&
                compatibility.getCompatibilityReports().isNativeModeAvailable() &&
                !serviceRunning.value){
                if(settings.nativeImmediateStart.get()){
                    if(!setService()) {
                        //Service was unable to be started for whatever reason, show notification
                        context.showNotification()
                    }
                }else{
                    context.showNotification()
                }
            }
        }
    }

    override fun onAsiStopped() {
        val now = System.currentTimeMillis()
        //Rate limit showing notifications / restarting
        if(now - lastShowedReconnectAt <= LAST_SHOWED_NOTIFICATION_RATE_LIMIT) return
        lastShowedReconnectAt = now
        scope.launch {
            if(settings.nativeImmediateStart.get()) {
                if(!reconnectService()) {
                    //Failed to reconnect automatically, show notification
                    context.showReconnectNotification(false)
                }
            }else{
                context.showReconnectNotification(false)
            }
        }
    }

    private suspend fun reconnectService(): Boolean {
        if(!resetService(onlyIfAvailable = false, killSystemUi = true)) return false
        delay(2500L)
        return setService()
    }

    override fun onFeedbackLoopDetected() {
        //Don't auto-reconnect for feedback loops to prevent repeatedly killing SystemUI in worst case
        context.showReconnectNotification(true)
    }

    private fun Context.showNotification() {
        notifications.showNotification(
            NotificationId.NATIVE_MODE,
            NotificationChannel.NATIVE_MODE
        ) {
            val notificationIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("smartspacer://native-from-notification")
                putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
            }
            val notificationJustEnableIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("smartspacer://native-from-notification")
                putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
                putExtra(NativeModeFragment.EXTRA_ENABLE_AND_FINISH, true)
            }
            it.setContentTitle(getString(R.string.notification_native_mode_enable_title))
            it.setContentText(getString(R.string.notification_native_mode_enable_content))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(false)
            val justEnableIntent = PendingIntent.getActivity(
                this,
                NotificationId.NATIVE_MODE_JUST_ENABLE.ordinal,
                notificationJustEnableIntent,
                PendingIntent_MUTABLE_FLAGS
            )
            it.addAction(0, getString(R.string.notification_native_mode_enable_now), justEnableIntent)
            val dismissIntent = PendingIntent.getBroadcast(
                this,
                NotificationId.NATIVE_MODE_DISMISS.ordinal,
                Intent(this, NativeDismissReceiver::class.java),
                PendingIntent_MUTABLE_FLAGS
            )
            it.addAction(0, getString(R.string.notification_native_mode_enable_dismiss), dismissIntent)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.NATIVE_MODE.ordinal,
                    notificationIntent,
                    PendingIntent_MUTABLE_FLAGS
                )
            )
            it.setTicker(getString(R.string.notification_native_mode_enable_title))
        }
    }

    private fun Context.showReconnectNotification(feedbackLoop: Boolean) {
        val content = if(feedbackLoop) {
            R.string.notification_native_mode_restart_feedback_loop_content
        }else{
            R.string.notification_native_mode_restart_disconnect_content
        }
        notifications.showNotification(
            NotificationId.RECONNECT_PROMPT,
            NotificationChannel.ERROR
        ) {
            val notificationIntent = NativeReconnectFragment.createIntent(context, feedbackLoop)
            val notificationJustReconnectIntent =
                NativeReconnectFragment.createIntent(context, feedbackLoop, true)
            it.setContentTitle(getString(R.string.notification_native_mode_restart_title))
            it.setContentText(getString(content))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(false)
            it.setAutoCancel(true)
            val justReconnectIntent = PendingIntent.getActivity(
                this,
                NotificationId.RECONNECT_JUST_RECONNECT.ordinal,
                notificationJustReconnectIntent,
                PendingIntent_MUTABLE_FLAGS
            )
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.RECONNECT_PROMPT.ordinal,
                    notificationIntent,
                    PendingIntent_MUTABLE_FLAGS
                )
            )
            it.addAction(0, getString(R.string.notification_native_mode_restart_just_reconnect), justReconnectIntent)
            it.setTicker(getString(R.string.notification_native_mode_restart_title))
        }
    }

}