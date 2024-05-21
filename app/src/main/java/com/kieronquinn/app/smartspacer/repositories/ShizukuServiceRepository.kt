package com.kieronquinn.app.smartspacer.repositories

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.ISmartspacerShizukuService
import com.kieronquinn.app.smartspacer.ISmartspacerSuiService
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository.ShizukuServiceResponse
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository.ShizukuServiceResponse.FailureReason
import com.kieronquinn.app.smartspacer.service.SmartspacerShizukuService
import com.kieronquinn.app.smartspacer.service.SmartspacerSuiService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface ShizukuServiceRepository {

    sealed class ShizukuServiceResponse<T> {
        data class Success<T>(val result: T): ShizukuServiceResponse<T>()
        data class Failed<T>(val reason: FailureReason): ShizukuServiceResponse<T>()

        enum class FailureReason {
            /**
             *  Shizuku is not bound, likely the user has not started it since rebooting
             */
            NO_BINDER,

            /**
             *  Permission to access Shizuku has not been granted
             */
            PERMISSION_DENIED,

            /**
             *  The service is not immediately available (only used in [runWithServiceIfAvailable])
             */
            NOT_AVAILABLE
        }

        /**
         *  Unwraps a result into either its value or null if it failed
         */
        fun unwrap(): T? {
            return (this as? Success)?.result
        }
    }

    val isReady: StateFlow<Boolean>
    val shizukuService: StateFlow<ISmartspacerShizukuService?>
    val suiService: StateFlow<ISmartspacerSuiService?>

    suspend fun assertReady(): Boolean
    suspend fun <T> runWithService(block: suspend (ISmartspacerShizukuService) -> T): ShizukuServiceResponse<T>
    fun <T> runWithServiceIfAvailable(block: (ISmartspacerShizukuService) -> T): ShizukuServiceResponse<T>
    suspend fun <T> runWithSuiService(block: suspend (ISmartspacerSuiService) -> T): ShizukuServiceResponse<T>
    fun <T> runWithSuiServiceIfAvailable(block: (ISmartspacerSuiService) -> T): ShizukuServiceResponse<T>
    fun disconnect()

}

class ShizukuServiceRepositoryImpl(
    context: Context,
    private val settingsRepository: SmartspacerSettingsRepository
): ShizukuServiceRepository, KoinComponent {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
        private const val SHIZUKU_TIMEOUT = 60_000L
    }

    private val shizukuComponent by lazy {
        ComponentName(context, SmartspacerShizukuService::class.java)
    }

    private val suiComponent by lazy {
        ComponentName(context, SmartspacerSuiService::class.java)
    }

    private val systemSmartspaceRepository by inject<SystemSmartspaceRepository>()

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(shizukuComponent).apply {
            daemon(false)
            debuggable(BuildConfig.DEBUG)
            version(BuildConfig.VERSION_CODE)
            processNameSuffix("shizuku")
        }
    }

    private val suiUserServiceArgs by lazy {
        Shizuku.UserServiceArgs(suiComponent).apply {
            daemon(false)
            debuggable(BuildConfig.DEBUG)
            version(BuildConfig.VERSION_CODE)
            processNameSuffix("sui")
        }
    }

    private var serviceConnection: ServiceConnection? = null
    private var suiServiceConnection: ServiceConnection? = null
    private val shizukuServiceLock = Mutex()
    private val suiServiceLock = Mutex()
    private val shizukuRunLock = Mutex()
    private val suiRunLock = Mutex()
    private val scope = MainScope()

    override val shizukuService = MutableStateFlow<ISmartspacerShizukuService?>(null)
    override val suiService = MutableStateFlow<ISmartspacerSuiService?>(null)

    private val binderReceived = callbackFlow {
        val listener = Shizuku.OnBinderReceivedListener {
            trySend(System.currentTimeMillis())
        }
        Shizuku.addBinderReceivedListener(listener)
        awaitClose {
            Shizuku.removeBinderReceivedListener(listener)
        }
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val binderDestroyed = callbackFlow {
        val listener = Shizuku.OnBinderDeadListener {
            //Clear service cache to sever the connection as we can't call unbind
            scope.launch {
                serviceConnection = null
                suiServiceConnection = null
                shizukuService.emit(null)
                suiService.emit(null)
                trySend(System.currentTimeMillis())
            }
        }
        Shizuku.addBinderDeadListener(listener)
        awaitClose {
            Shizuku.removeBinderDeadListener(listener)
        }
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val binderReady = combine(binderReceived, binderDestroyed) { _, _ ->
        Shizuku.pingBinder()
    }.stateIn(scope, SharingStarted.Eagerly, Shizuku.pingBinder())

    override val isReady = combine(
        settingsRepository.enhancedMode.asFlow(),
        binderReady
    ) { enabled, _ ->
        if(!enabled) return@combine false
        assertReady()
    }.onEach {
        if(it){
            systemSmartspaceRepository.showNativeStartReminderIfNeeded()
            updateUsernameIfNeeded()
            //Start the service if required
            getService()
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun assertReady(): Boolean {
        val rawResult = runWithService {
            it.ping()
        }
        val result = rawResult.unwrap()
        return result == true
    }

    override suspend fun <T> runWithService(
        block: suspend (ISmartspacerShizukuService) -> T
    ): ShizukuServiceResponse<T> = shizukuRunLock.withLock {
        return runWithServiceLocked(block)
    }

    override suspend fun <T> runWithSuiService(
        block: suspend (ISmartspacerSuiService) -> T
    ): ShizukuServiceResponse<T> = suiRunLock.withLock {
        runWithSuiServiceLocked(block)
    }

    private suspend fun <T> runWithServiceLocked(
        block: suspend (ISmartspacerShizukuService) -> T
    ): ShizukuServiceResponse<T> = withTimeout(SHIZUKU_TIMEOUT) {
        shizukuService.value?.let {
            if(!it.safePing()){
                //Service has disconnected or died
                shizukuService.emit(null)
                serviceConnection = null
                return@let
            }
            val result = try {
                block(it)
            }catch (e: RuntimeException){
                return@withTimeout ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
            }
            return@withTimeout ShizukuServiceResponse.Success(result)
        }
        if(!Shizuku.pingBinder())
            return@withTimeout ShizukuServiceResponse.Failed(FailureReason.NO_BINDER)
        if(!requestPermission())
            return@withTimeout ShizukuServiceResponse.Failed(FailureReason.PERMISSION_DENIED)
        val result = try {
            block(getService()
                ?: return@withTimeout ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE))
        }catch (e: RuntimeException){
            return@withTimeout ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }
        return@withTimeout ShizukuServiceResponse.Success(result)
    }

    private suspend fun <T> runWithSuiServiceLocked(
        block: suspend (ISmartspacerSuiService) -> T
    ): ShizukuServiceResponse<T> = withTimeout(SHIZUKU_TIMEOUT) {
        suiService.value?.let {
            if(!it.safePing()){
                //Service has disconnected or died
                suiService.emit(null)
                suiServiceConnection = null
                return@let
            }
            val result = try {
                block(it)
            }catch (e: RuntimeException){
                return@withTimeout ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
            }
            return@withTimeout ShizukuServiceResponse.Success(result)
        }
        if(!Shizuku.pingBinder())
            return@withTimeout ShizukuServiceResponse.Failed(FailureReason.NO_BINDER)
        if(!requestPermission())
            return@withTimeout ShizukuServiceResponse.Failed(FailureReason.PERMISSION_DENIED)
        val result = try {
            block(getSuiService()
                ?: return@withTimeout ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE))
        }catch (e: RuntimeException){
            return@withTimeout ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }
        return@withTimeout ShizukuServiceResponse.Success(result)
    }

    override fun <T> runWithServiceIfAvailable(
        block: (ISmartspacerShizukuService) -> T
    ): ShizukuServiceResponse<T> {
        return try {
            shizukuService.value?.let {
                ShizukuServiceResponse.Success(block(it))
            } ?: ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }catch (e: RuntimeException){
            ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }
    }

    override fun <T> runWithSuiServiceIfAvailable(
        block: (ISmartspacerSuiService) -> T
    ): ShizukuServiceResponse<T> {
        return try {
            suiService.value?.let {
                ShizukuServiceResponse.Success(block(it))
            } ?: ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }catch (e: RuntimeException){
            ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }
    }

    override fun disconnect() {
        serviceConnection?.let {
            Shizuku.unbindUserService(userServiceArgs, it, true)
        }
        suiServiceConnection?.let {
            Shizuku.unbindUserService(suiUserServiceArgs, it, true)
        }
    }

    private suspend fun requestPermission() = suspendCancellableCoroutine {
        var hasResumed = false
        if(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            if(!hasResumed) {
                hasResumed = true
                it.resume(true) //Already granted
            }
            return@suspendCancellableCoroutine
        }
        val listener = object: Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if(requestCode != SHIZUKU_PERMISSION_REQUEST_CODE) return
                Shizuku.removeRequestPermissionResultListener(this)
                if(!hasResumed) {
                    hasResumed = true
                    it.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        it.invokeOnCancellation {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }

    private suspend fun getService() = shizukuServiceLock.withLock {
        suspendCoroutine {
            var hasResumed = false
            val serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                    serviceConnection = this
                    val service = ISmartspacerShizukuService.Stub.asInterface(binder)
                    scope.launch {
                        this@ShizukuServiceRepositoryImpl.shizukuService.emit(service)
                        if(!hasResumed){
                            hasResumed = true
                            try {
                                it.resume(service)
                            }catch (e: IllegalStateException) {
                                //Already resumed
                            }
                        }
                    }
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    serviceConnection = null
                    scope.launch {
                        shizukuService.emit(null)
                    }
                }
            }
            try {
                Shizuku.bindUserService(userServiceArgs, serviceConnection)
            }catch (e: RuntimeException) {
                //Shizuku died
                it.resume(null)
            }
        }
    }

    private suspend fun getSuiService() = suiServiceLock.withLock {
        suspendCoroutine {
            var hasResumed = false
            val serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                    suiServiceConnection = this
                    val service = ISmartspacerSuiService.Stub.asInterface(binder)
                    scope.launch {
                        suiService.emit(service)
                        if(!hasResumed){
                            hasResumed = true
                            it.resume(service)
                        }
                    }
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    suiServiceConnection = null
                    scope.launch {
                        suiService.emit(null)
                    }
                }
            }
            try {
                Shizuku.bindUserService(suiUserServiceArgs, serviceConnection)
            }catch (e: RuntimeException) {
                //Shizuku died
                it.resume(null)
            }
        }
    }

    private fun ISmartspacerShizukuService.safePing(): Boolean {
        return try {
            ping()
        }catch (e: RemoteException){
            false
        }
    }

    private fun ISmartspacerSuiService.safePing(): Boolean {
        return try {
            ping()
        }catch (e: RemoteException){
            false
        }
    }

    /**
     *  Gets and updates the username in the local settings, which is used as the default in the
     *  Greeting target.
     */
    private suspend fun updateUsernameIfNeeded() {
        val userName = runWithService { it.userName }.unwrap() ?: return
        settingsRepository.userName.set(userName)
    }

}