package com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence

import app.cash.turbine.test
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GeofenceRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.GeofenceRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomDouble
import com.kieronquinn.app.smartspacer.utils.randomFloat
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.math.roundToInt

class GeofenceRequirementConfigurationViewModelTests: BaseTest<GeofenceRequirementConfigurationViewModel>() {

    private val geofenceRepositoryMock = mock<GeofenceRepository> {
        every { geofenceRequirements } returns MutableStateFlow(emptyList())
    }

    private val databaseRepositoryMock = mock<DatabaseRepository>()

    override val sut by lazy {
        GeofenceRequirementConfigurationViewModelImpl(
            contextMock,
            geofenceRepositoryMock,
            databaseRepositoryMock,
            Gson(),
            scope,
            Dispatchers.Main
        )
    }

    private val mockId = randomString()

    override fun setup() {
        super.setup()
        every { geofenceRepositoryMock.hasLocationPermission() } returns true
        every { geofenceRepositoryMock.hasBackgroundLocationPermission() } returns true
        coEvery { geofenceRepositoryMock.isGeofenceLimitReached() } returns false
    }

    @Test
    fun testState() = runTest {
        every { geofenceRepositoryMock.hasLocationPermission() } returns false
        every { geofenceRepositoryMock.hasBackgroundLocationPermission() } returns false
        coEvery { geofenceRepositoryMock.isGeofenceLimitReached() } returns true
        sut.state.test {
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.RequestPermission)
            every { geofenceRepositoryMock.hasLocationPermission() } returns true
            sut.onResumed()
            assertTrue(awaitItem() is State.RequestBackgroundPermission)
            every { geofenceRepositoryMock.hasBackgroundLocationPermission() } returns true
            sut.onResumed()
            assertTrue(awaitItem() is State.LimitReached)
            coEvery { geofenceRepositoryMock.isGeofenceLimitReached() } returns false
            sut.onResumed()
            assertTrue(awaitItem() is State.Loading)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.data.id == mockId)
        }
    }

    @Test
    fun testOnLatLngChanged() = runTest {
        sut.state.test {
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.Loading)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val latlng = randomLatLng()
            sut.onLatLngChanged(latlng)
            assertTrue(
                item.data.latitude == latlng.latitude && item.data.longitude == latlng.longitude
            )
        }
    }

    @Test
    fun testOnNameChanged() = runTest {
        sut.state.test {
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.Loading)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val name = randomString()
            sut.onNameChanged(name)
            assertTrue(item.data.name == name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnRadiusChanged() = runTest {
        sut.state.test {
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.Loading)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val radius = randomFloat()
            sut.radius.test {
                sut.onRadiusChanged(radius)
                assertTrue(item.data.radius == radius)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun testOnLoiteringDelayChanged() = runTest {
        sut.state.test {
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.Loading)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val delay = randomFloat()
            sut.onLoiteringDelayChanged(delay)
            assertTrue(item.data.loiteringDelay == delay.roundToInt())
        }
    }

    @Test
    fun testOnSaveClicked() = runTest {
        sut.state.test {
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.Loading)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onNameChanged(randomString())
            sut.dismissBus.test {
                sut.onSaveClicked()
                coVerify {
                    databaseRepositoryMock.addRequirementData(any())
                }
                assertTrue(awaitItem() == Unit)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testBottomSheetOffset() = runTest {
        sut.bottomSheetOffset.test {
            assertTrue(awaitItem() == 0f)
            sut.setBottomSheetOffset(0.5f)
            sut.setBottomSheetInset(1.5f)
            assertTrue(awaitItem() == 0.5f)
        }
    }

    private fun randomLatLng() = LatLng(randomLatitude(), randomLongitude())

    private fun randomLatitude() = randomDouble(-90.0, 90.0)
    private fun randomLongitude() = randomDouble(-180.0, 180.0)

}