package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.os.Build
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.Smartspacer.Companion.PACKAGE_KEYGUARD
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DefaultComplication
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.AppPredictionRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.RecentTaskRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.FlashlightTarget
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Compatibility
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport.Companion.PACKAGE_PIXEL_LAUNCHER
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityState
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Feature
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Template
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.HeadToHeadTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubCardTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubListTemplateData
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.getAllLaunchers
import com.kieronquinn.app.smartspacer.utils.extensions.getAppPredictionComponent
import com.kieronquinn.app.smartspacer.utils.extensions.getClassLoaderForPackage
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultSmartspaceComponent
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.isAtLeastBaklava
import com.kieronquinn.app.smartspacer.utils.extensions.isAtLeastU
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface CompatibilityRepository {

    /**
     *  Emits a list of compatibility reports for packages which support native Smartspace on this
     *  device.
     *
     *  Known packages are currently SystemUI and some launchers, will always return an empty
     *  list on < 12.
     */
    val compatibilityReports: Flow<List<CompatibilityReport>>

    /**
     *  Returns the current [CompatibilityReport] list, awaiting the first load if required.
     */
    suspend fun getCompatibilityReports(): List<CompatibilityReport>

    /**
     *  Fast response version of [getCompatibilityState] that returns if **any** item is supported,
     *  this should be used to offer enhanced mode to the user where applicable
     */
    suspend fun isEnhancedModeAvailable(): Boolean

    /**
     *  Get the [CompatibilityState] for this device, which contains the compatibility data for
     *  native, launcher(s), lock screen and app prediction
     */
    suspend fun getCompatibilityState(skipOem: Boolean = false): CompatibilityState

    /**
     *  Returns whether the build of SystemUI contains `WeatherSmartspaceView` and that the Split
     *  Smartspace prop isn't disabled, indicating Split Smartspace is supported. This is required
     *  as some Android 14 ROMs currently use 13's code, which is missing Split support, and
     *  therefore we can't simply do an SDK check
     */
    fun doesSystemUISupportSplitSmartspace(): Boolean

    data class CompatibilityState(
        val systemSupported: Boolean,
        val anyLauncherSupported: Boolean,
        val lockscreenSupported: Boolean,
        val appPredictionSupported: Boolean,
        val oemSmartspaceSupported: Boolean
    )

    data class CompatibilityReport(
        val packageName: String,
        val label: CharSequence,
        val compatibility: List<Compatibility>
    ) {

        companion object {
            internal const val PACKAGE_PIXEL_LAUNCHER = "com.google.android.apps.nexuslauncher"

            fun List<CompatibilityReport>.isNativeModeAvailable(): Boolean {
                //No compatible apps found
                if(isEmpty()) return false
                //Compatible apps must have at least one compatible Template or Feature available.
                return any { it.compatibility.any { compatible -> compatible.compatible } }
            }

            fun List<CompatibilityReport>.areRemoteViewsSupported(): Boolean {
                //No compatible apps found
                if(isEmpty()) return false
                return any {
                    it.compatibility.any { compatible ->
                        compatible.compatible && compatible.item == Feature.REMOTE_VIEWS
                    } || (it.packageName == PACKAGE_PIXEL_LAUNCHER && isAtLeastBaklava())
                }
            }
        }

    }

    data class Compatibility(
        val item: Base,
        val compatible: Boolean
    )

    interface Base {
        val title: Int
        val content: Int
    }

    enum class Feature(
        private val className: String,
        override val title: Int,
        override val content: Int,
        vararg val feature: Int? = emptyArray()
    ): Base {
        BASIC(
            "BcSmartspaceCard",
            R.string.compatibility_feature_basic_title,
            R.string.compatibility_feature_basic_content
        ),
        COMBINATION(
            "BcSmartspaceCardCombination",
            R.string.compatibility_feature_combination_title,
            R.string.compatibility_feature_combination_content,
            SmartspaceTarget.FEATURE_COMBINATION
        ),
        COMBINATION_AT_STORE(
            "BcSmartspaceCardCombinationAtStore",
            R.string.compatibility_feature_combination_at_store_title,
            R.string.compatibility_feature_combination_at_store_content,
            SmartspaceTarget.FEATURE_COMBINATION_AT_STORE
        ),
        DOORBELL(
            "BcSmartspaceCardDoorbell",
            R.string.compatibility_feature_doorbell_title,
            R.string.compatibility_feature_doorbell_content,
            SmartspaceTarget.FEATURE_DOORBELL,
            SmartspaceTarget.FEATURE_PACKAGE_TRACKING
        ),
        FLIGHT(
            "BcSmartspaceCardFlight",
            R.string.compatibility_feature_flight_title,
            R.string.compatibility_feature_flight_content,
            SmartspaceTarget.FEATURE_FLIGHT
        ),
        IMAGE(
            "BcSmartspaceCardGenericImage",
            R.string.compatibility_feature_image_title,
            R.string.compatibility_feature_image_content,
            SmartspaceTarget.FEATURE_COMMUTE_TIME,
            SmartspaceTarget.FEATURE_ETA_MONITORING
        ),
        LOYALTY(
            "BcSmartspaceCardLoyalty",
            R.string.compatibility_feature_loyalty_title,
            R.string.compatibility_feature_loyalty_content,
            SmartspaceTarget.FEATURE_LOYALTY_CARD
        ),
        REMOTE_VIEWS(
            "BcSmartspaceRemoteViewsCard",
            R.string.compatibility_feature_remoteviews_title,
            R.string.compatibility_feature_remoteviews_content
        ),
        SHOPPING_LIST(
            "BcSmartspaceCardShoppingList",
            R.string.compatibility_feature_shopping_list_title,
            R.string.compatibility_feature_shopping_list_content,
            SmartspaceTarget.FEATURE_SHOPPING_LIST
        ),
        SPORTS(
            "BcSmartspaceCardSports",
            R.string.compatibility_feature_sports_title,
            R.string.compatibility_feature_sports_content,
            SmartspaceTarget.FEATURE_SPORTS
        );

        companion object {
            private const val BASE_PACKAGE = "com.google.android.systemui.smartspace"
        }

        fun getClassName(): String {
            return "${BASE_PACKAGE}.$className"
        }
    }

    enum class Template(
        private val className: String,
        val smartspacerTemplate: Class<out BaseTemplateData>,
        override val title: Int,
        override val content: Int
    ): Base {
        BASE(
            "BaseTemplateCard",
            BaseTemplateData::class.java,
            R.string.compatibility_template_base_title,
            R.string.compatibility_template_base_content
        ),
        CAROUSEL(
            "CarouselTemplateCard",
            CarouselTemplateData::class.java,
            R.string.compatibility_template_carousel_title,
            R.string.compatibility_template_carousel_content
        ),
        HEAD_TO_HEAD(
            "HeadToHeadTemplateCard",
            HeadToHeadTemplateData::class.java,
            R.string.compatibility_template_head_to_head_title,
            R.string.compatibility_template_head_to_head_content
        ),
        SUB_CARD(
            "SubCardTemplateCard",
            SubCardTemplateData::class.java,
            R.string.compatibility_template_sub_card_title,
            R.string.compatibility_template_sub_card_content
        ),
        SUB_IMAGE(
            "SubImageTemplateCard",
            SubImageTemplateData::class.java,
            R.string.compatibility_template_sub_image_title,
            R.string.compatibility_template_sub_images_content
        ),
        SUB_LIST(
            "SubListTemplateCard",
            SubListTemplateData::class.java,
            R.string.compatibility_template_sub_list_title,
            R.string.compatibility_template_sub_list_content
        );

        companion object {
            private const val BASE_PACKAGE = "com.google.android.systemui.smartspace.uitemplate"
        }

        fun getClassName(): String {
            return "${BASE_PACKAGE}.$className"
        }
    }

}

class CompatibilityRepositoryImpl(
    private val context: Context,
    private val targetsRepository: TargetsRepository,
    private val requirementsRepository: RequirementsRepository,
    packageRepository: PackageRepository,
    settings: SmartspacerSettingsRepository,
    private val scope: CoroutineScope = MainScope()
): CompatibilityRepository, KoinComponent {

    companion object {
        private val FEATURES_WITH_TEMPLATES = arrayOf(
            Feature.BASIC,
            Feature.COMBINATION,
            Feature.SHOPPING_LIST,
            Feature.SPORTS
        )

        private val ENHANCED_MODE_TARGETS = arrayOf(
            DefaultTarget::class.java,
            FlashlightTarget::class.java //Allows direct toggle rather than activity launch
        )

        private val ENHANCED_MODE_COMPLICATIONS = arrayOf(
            DefaultComplication::class.java
        )

        private val ENHANCED_MODE_REQUIREMENTS = arrayOf(
            AppPredictionRequirement::class.java,
            RecentTaskRequirement::class.java
        )

        private const val CLASS_NAME_SPLIT_WEATHER =
            "com.google.android.systemui.smartspace.WeatherSmartspaceView"
    }

    private val enhancedMode = settings.enhancedMode.asFlow()
    private val allLaunchers = context.getAllLaunchers()
    private val oemSmartspacerRepository by inject<OemSmartspacerRepository>()

    private val packageChanged = packageRepository
        .onPackageChanged(scope, *allLaunchers.toTypedArray(), PACKAGE_KEYGUARD)
        .stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val _compatibilityReports = packageChanged.mapLatest {
        //Never supported on < 12
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) return@mapLatest emptyList()
        //Lock Screen
        val systemUi = getCompatibleItemsForPackage(PACKAGE_KEYGUARD).let {
            if(it.isEmpty()) return@let null
            CompatibilityReport(
                PACKAGE_KEYGUARD,
                context.getString(R.string.compatibility_label_systemui),
                it
            )
        }
        val launchers = allLaunchers.mapNotNull { packageName ->
            val label = context.packageManager.getPackageLabel(packageName)
                ?: return@mapNotNull null
            getCompatibleItemsForPackage(packageName).let {
                if(it.isEmpty()) return@let null
                CompatibilityReport(packageName, label, it)
            }
        }.filterNot {
            //Filter out launchers with nothing compatible
            it.compatibility.none { compatibility -> compatibility.compatible }
        }.toTypedArray()
        listOfNotNull(systemUi, *launchers)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override val compatibilityReports = _compatibilityReports.filterNotNull()

    override suspend fun getCompatibilityReports(): List<CompatibilityReport> {
        return _compatibilityReports.firstNotNull()
    }

    override suspend fun isEnhancedModeAvailable(): Boolean {
        //Faster checks first to save time on devices with more support
        if(context.getDefaultSmartspaceComponent() != null) return true
        if(context.getAppPredictionComponent() != null) return true
        if(getCompatibilityReports().isNotEmpty()) return true
        return oemSmartspacerRepository.getCompatibleApps().first().isNotEmpty()
    }

    override suspend fun getCompatibilityState(skipOem: Boolean): CompatibilityState {
        val systemSupported = context.getDefaultSmartspaceComponent() != null
        val appPredictionSupported = context.getAppPredictionComponent() != null
        val reports = getCompatibilityReports()
        val anyLauncherSupported = reports.any { it.packageName != PACKAGE_KEYGUARD }
        val lockscreenSupported = reports.any { it.packageName == PACKAGE_KEYGUARD }
        val oemSmartspaceSupported = if(skipOem) false else {
            oemSmartspacerRepository.getCompatibleApps().first().isNotEmpty()
        }
        return CompatibilityState(
            systemSupported,
            anyLauncherSupported,
            lockscreenSupported,
            appPredictionSupported,
            oemSmartspaceSupported
        )
    }

    private suspend fun getCompatibleItemsForPackage(packageName: String): List<Compatibility> {
        //Only Android 12+ supports system Smartspace at all
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) return emptyList()
        //Only Android 13+ actually supports templates, they should not be considered on 12 or below
        val supportedTemplates = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSupportedTemplates(packageName)
        }else emptyList()
        val supportedFeatures = getSupportedFeatures(packageName)
        //Prefer templates if any are supported
        return if(supportedTemplates.isNotEmpty() && supportedTemplates.any { it.compatible }){
            //Filter out the features duplicated by templates
            supportedTemplates + supportedFeatures.filterNot {
                FEATURES_WITH_TEMPLATES.contains(it.item)
            }
        }else{
            //Return only the supported features
            supportedFeatures
        }
    }

    private suspend fun getSupportedFeatures(
        packageName: String
    ): List<Compatibility> = withContext(Dispatchers.IO) {
        val classLoader = context.getClassLoaderForPackage(packageName)
            ?: return@withContext emptyList()
        Feature.entries.map {
            if(it == Feature.REMOTE_VIEWS && packageName == PACKAGE_PIXEL_LAUNCHER && isAtLeastBaklava()) {
                Compatibility(it, true)
            }else{
                Compatibility(it, classLoader.exists(it.getClassName()))
            }
        }
    }

    private suspend fun getSupportedTemplates(
        packageName: String
    ): List<Compatibility> = withContext(Dispatchers.IO) {
        val classLoader = context.getClassLoaderForPackage(packageName)
            ?: return@withContext emptyList()
        Template.entries.map {
            Compatibility(it, classLoader.exists(it.getClassName()))
        }
    }

    override fun doesSystemUISupportSplitSmartspace(): Boolean {
        if(!isAtLeastU()) return false
        val classLoader = context.getClassLoaderForPackage(PACKAGE_KEYGUARD) ?: return false
        return classLoader.exists(CLASS_NAME_SPLIT_WEATHER)
    }

    private fun ClassLoader.exists(className: String): Boolean {
        return try {
            loadClass(className)
            true
        }catch (e: ClassNotFoundException){
            false
        }
    }

    private fun setupEnhancedModeNotifications() = scope.launch {
        enhancedMode.collect {
            notifyEnhancedModeChange()
        }
    }

    private suspend fun notifyEnhancedModeChange() = withContext(Dispatchers.IO) {
        ENHANCED_MODE_TARGETS.forEach {
            SmartspacerTargetProvider.notifyChange(context, it)
        }
        ENHANCED_MODE_COMPLICATIONS.forEach {
            SmartspacerComplicationProvider.notifyChange(context, it)
        }
        ENHANCED_MODE_REQUIREMENTS.forEach {
            SmartspacerRequirementProvider.notifyChange(context, it)
        }
        targetsRepository.forceReloadAll()
        requirementsRepository.forceReloadAll()
    }

    init {
        setupEnhancedModeNotifications()
    }

}