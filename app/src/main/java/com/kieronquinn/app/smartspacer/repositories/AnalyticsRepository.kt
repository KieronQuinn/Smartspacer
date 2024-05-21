package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.os.Build
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 *  Controls whether Firebase Crashlytics + Analytics are enabled based on the setting, which is
 *  disabled by default. If the user opts in, this is automatically enabled and will re-run on
 *  start, unless they disable the setting.
 */
interface AnalyticsRepository

class AnalyticsRepositoryImpl(
    private val context: Context,
    private val settingsRepository: SmartspacerSettingsRepository
): AnalyticsRepository {

    private val scope = MainScope()

    private fun setupState() = scope.launch {
        settingsRepository.analyticsEnabled.asFlow().collect {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(it)
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(false)
                setupCrashlytics()
            }
        }
    }

    private fun FirebaseCrashlytics.setupCrashlytics() {
        setCustomKeys {
            key("fingerprint", Build.FINGERPRINT)
        }
    }

    init {
        setupState()
    }

}