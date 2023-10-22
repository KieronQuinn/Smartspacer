package com.kieronquinn.app.smartspacer.repositories

import android.app.prediction.AppTarget
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.IAppPredictionOnTargetsAvailableListener
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.AppPredictionRequirement.AppPredictionRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.mockParceledListSlice
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppPredictionRepositoryTests: BaseTest<AppPredictionRepository>() {

    companion object {
        private fun createMockAppTarget(packageName: String, rank: Int): AppTarget {
            return mock {
                every { getPackageName() } returns packageName
                every { getRank() } returns rank
            }
        }

        private fun createMockAppTargets(): List<AppTarget> {
            return listOf(
                createMockAppTarget("com.example.one", 2),
                createMockAppTarget("com.example.two", 3),
                createMockAppTarget("com.example.three", 1)
            )
        }

        private fun createVariantMockAppTargets(): List<AppTarget> {
            return listOf(
                createMockAppTarget("com.example.four", 2),
                createMockAppTarget("com.example.five", 3),
                createMockAppTarget("com.example.six", 1)
            )
        }

        private const val FAKE_SERVICE_RESOURCE_ID = 0xCAFE
        private const val FAKE_SERVICE_COMPONENT = "com.example.app/com.example.app.component"
        private const val AUTHORITY_APP_PREDICTION =
            "${BuildConfig.APPLICATION_ID}.requirement.appprediction"
    }

    private var mockPredictionListener: IAppPredictionOnTargetsAvailableListener? = null

    private val shizukuRepositoryMock = mockShizukuRepository {
        val callbackCapture = slot<IAppPredictionOnTargetsAvailableListener>()
        every { createAppPredictorSession(capture(callbackCapture)) } answers {
            mockPredictionListener = callbackCapture.captured
        }
        every { destroyAppPredictorSession() } answers {
            mockPredictionListener = null
        }
    }

    private val dataRepositoryMock = mock<DataRepository>()

    override val sut by lazy {
        AppPredictionRepositoryImpl(
            contextMock,
            shizukuRepositoryMock,
            dataRepositoryMock,
            scope
        )
    }

    @Test
    fun verifyAppPredictionsEmptyWhenShizukuNotReady() = runTest {
        every { shizukuRepositoryMock.isReady } returns MutableStateFlow(false)
        sut.appPredictions.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun verifyAppPredictionsEmptyReturnedWithNoSuggestions() = runTest {
        every { shizukuRepositoryMock.isReady } returns MutableStateFlow(true)
        sut.appPredictions.test {
            mockPredictionListener!!.onTargetsAvailable(mockParceledListSlice<AppTarget>())
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun verifyAppPredictionsReturnedInOrder() = runTest {
        every { shizukuRepositoryMock.isReady } returns MutableStateFlow(true)
        val mockTargets = createMockAppTargets()
        sut.appPredictions.test {
            assertTrue(awaitItem().isEmpty())
            mockPredictionListener!!.onTargetsAvailable(mockParceledListSlice(mockTargets))
            val expected = mockTargets.sortedBy { it.rank }.map { it.packageName }
            assertTrue(awaitItem() == expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun verifyNotSupportedIfNotReady() = runTest {
        every { shizukuRepositoryMock.isReady } returns MutableStateFlow(false)
        assertFalse(sut.isSupported())
    }

    @Test
    fun verifyNotSupportedIfNoComponent() = runTest {
        every { shizukuRepositoryMock.isReady } returns MutableStateFlow(true)
        every {
            resourcesMock.getIdentifier(
                "config_defaultAppPredictionService", "string", "android"
            )
        } returns FAKE_SERVICE_RESOURCE_ID
        every { resourcesMock.getString(FAKE_SERVICE_RESOURCE_ID) } returns ""
        assertFalse(sut.isSupported())
    }

    @Test
    fun verifySupported() = runTest {
        every { shizukuRepositoryMock.isReady } returns MutableStateFlow(true)
        every {
            resourcesMock.getIdentifier(
                "config_defaultAppPredictionService", "string", "android"
            )
        } returns FAKE_SERVICE_RESOURCE_ID
        every { resourcesMock.getString(FAKE_SERVICE_RESOURCE_ID) } returns FAKE_SERVICE_COMPONENT
        every {
            packageManagerMock.getServiceInfo(any(), any<ComponentInfoFlags>())
        } returns mock()
        assertTrue(sut.isSupported())
    }

    @Test
    fun verifyListenerIsCalled() = runTest {
        every { shizukuRepositoryMock.isReady } returns MutableStateFlow(true)
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_APP_PREDICTION
            }
        }
        val oneRequirement = AppPredictionRequirementData("one", "com.example.one")
        val fourRequirement = AppPredictionRequirementData("four", "com.example.four")
        every {
            dataRepositoryMock.getRequirementData(
                RequirementDataType.APP_PREDICTION, AppPredictionRequirementData::class.java
            )
        } answers {
            flowOf(listOf(oneRequirement, fourRequirement))
        }
        val uriSlot = slot<Uri>()
        sut
        every { contentResolverMock.notifyChange(capture(uriSlot), any(), any<Int>()) } just Runs
        val mockTargets = createMockAppTargets()
        val variantMockTargets = createVariantMockAppTargets()
        sut.setupListener()
        val uriOne = getUriForId("one")
        val uriFour = getUriForId("four")
        runBlocking {
            delay(1000L)
            mockPredictionListener!!.onTargetsAvailable(mockParceledListSlice(mockTargets))
            delay(1000L)
            verify {
                contentResolverMock.notifyChange(any<Uri>(), null, 0)
            }
            assertTrue(uriOne.toString() == uriSlot.captured.toString())
            mockPredictionListener!!.onTargetsAvailable(mockParceledListSlice(variantMockTargets))
            delay(1000L)
            assertTrue(uriFour.toString() == uriSlot.captured.toString())
        }
    }

    private fun getUriForId(id: String): Uri {
        return Uri.Builder().apply {
            scheme("content")
            authority(AUTHORITY_APP_PREDICTION)
            appendPath(id)
        }.build()
    }

}