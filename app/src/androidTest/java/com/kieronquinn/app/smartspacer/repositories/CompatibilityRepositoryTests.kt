package com.kieronquinn.app.smartspacer.repositories

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import android.util.Log
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Base
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Compatibility
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityState
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Feature
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Template
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.PACKAGE_PIXEL_LAUNCHER
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomString
import dalvik.system.PathClassLoader
import io.mockk.Matcher
import io.mockk.OfTypeMatcher
import io.mockk.every
import io.mockk.mockkConstructor
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration

class CompatibilityRepositoryTests: BaseTest<CompatibilityRepository>() {

    companion object {
        private fun getMockCompatibilityReport(packageName: String): CompatibilityReport {
            return CompatibilityReport(packageName, randomString(), getMockCompatibility())
        }

        private fun getMockCompatibility(): List<Compatibility> {
            return getMockFeatures() + getMockTemplates()
        }

        private fun getMockFeatures(): List<Compatibility> {
            return Feature.values().map {
                Compatibility(it, randomBoolean())
            }
        }

        private fun getMockTemplates(): List<Compatibility> {
            return Template.values().map {
                Compatibility(it, randomBoolean())
            }
        }
    }

    private val targetsRepositoryMock = mock<TargetsRepository>()
    private val requirementsRepositoryMock = mock<RequirementsRepository>()
    private val packageRepositoryMock = mock<PackageRepository>()
    private val settingsMock = mock<SmartspacerSettingsRepository>()

    private val pixelLauncherReports =
        getMockCompatibilityReport("com.google.android.apps.nexuslauncher")

    private val systemUiReports = getMockCompatibilityReport("com.android.systemui")

    override val sut by lazy {
        CompatibilityRepositoryImpl(
            contextMock,
            targetsRepositoryMock,
            requirementsRepositoryMock,
            packageRepositoryMock,
            settingsMock,
            scope
        )
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getApplicationInfo(any(), any<ApplicationInfoFlags>()) } answers {
            ApplicationInfo().apply {
                sourceDir = firstArg()
            }
        }
        every { packageManagerMock.queryIntentActivities(any(), any<ResolveInfoFlags>()) } answers {
            listOf(
                ResolveInfo().apply {
                    activityInfo = ActivityInfo().apply {
                        packageName = PACKAGE_PIXEL_LAUNCHER
                    }
                }
            )
        }
        val systemUiMatcher = object: Matcher<String> {
            override fun match(arg: String?): Boolean {
                return arg == "com.android.systemui"
            }
        }
        val pixelLauncherMatcher = object: Matcher<String> {
            override fun match(arg: String?): Boolean {
                return arg == PACKAGE_PIXEL_LAUNCHER
            }
        }
        mockkConstructor(PathClassLoader::class)
        every {
            constructedWith<PathClassLoader>(
                systemUiMatcher, OfTypeMatcher<ClassLoader>(ClassLoader::class)
            ).loadClass(any())
        } answers {
            val item = systemUiReports.compatibility.first {
                it.item.className() == firstArg()
            }
            if(item.compatible){
                this::class.java
            }else{
                throw ClassNotFoundException()
            }
        }
        every {
            constructedWith<PathClassLoader>(
                pixelLauncherMatcher, OfTypeMatcher<ClassLoader>(ClassLoader::class)
            ).loadClass(any())
        } answers {
            val item = pixelLauncherReports.compatibility.first {
                it.item.className() == firstArg()
            }
            if(item.compatible){
                this::class.java
            }else{
                throw ClassNotFoundException()
            }
        }
    }

    private fun Base.className(): String {
        return when(this) {
            is Feature -> getClassName()
            is Template -> getClassName()
            else -> throw RuntimeException("Unknown item ${javaClass.simpleName}")
        }
    }

    @Test
    fun testCompatibilityReports() = runTest {
        sut.compatibilityReports.test(timeout = Duration.INFINITE) {
            val reports = awaitItem()
            Log.d("CR", "Reports: $reports")
            val actualPixelLauncherReport = reports.first {
                it.packageName == PACKAGE_PIXEL_LAUNCHER
            }
            val actualSystemUIReport = reports.first {
                it.packageName == "com.android.systemui"
            }
            actualPixelLauncherReport.compatibility.forEach { actual ->
                val mock = pixelLauncherReports.compatibility.first { it.item == actual.item }
                assertTrue(mock.compatible == actual.compatible)
            }
            actualSystemUIReport.compatibility.forEach { actual ->
                val mock = systemUiReports.compatibility.first { it.item == actual.item }
                assertTrue(mock.compatible == actual.compatible)
            }
        }
    }

    @Test
    fun testGetCompatibilityReports() = runTest {
        sut.getCompatibilityReports().let { reports ->
            val actualPixelLauncherReport = reports.first {
                it.packageName == PACKAGE_PIXEL_LAUNCHER
            }
            val actualSystemUIReport = reports.first {
                it.packageName == "com.android.systemui"
            }
            actualPixelLauncherReport.compatibility.forEach { actual ->
                val mock = pixelLauncherReports.compatibility.first { it.item == actual.item }
                assertTrue(mock.compatible == actual.compatible)
            }
            actualSystemUIReport.compatibility.forEach { actual ->
                val mock = systemUiReports.compatibility.first { it.item == actual.item }
                assertTrue(mock.compatible == actual.compatible)
            }
        }
    }

    @Test
    fun testCompatibilityState() = runTest {
        every { resourcesMock.getIdentifier(any(), any(), any()) } returns 0
        val actual = sut.getCompatibilityState(true)
        val expected = CompatibilityState(
            systemSupported = false,
            anyLauncherSupported = true,
            lockscreenSupported = true,
            appPredictionSupported = false,
            oemSmartspaceSupported = false
        )
        assertTrue(expected == actual)
    }

}