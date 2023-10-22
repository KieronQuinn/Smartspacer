package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.GeofenceRequirement.GeofenceRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomDouble
import com.kieronquinn.app.smartspacer.utils.randomFloat
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Locale

class GeofenceRepositoryTests: BaseTest<GeofenceRepository>() {

    companion object {
        private fun getMockGeofences(size: Int = 1): List<GeofenceRequirementData> {
            val list = ArrayList<GeofenceRequirementData>()
            repeat(size) {
                list.add(
                    GeofenceRequirementData(
                        randomString(),
                        randomLatitude(),
                        randomLongitude(),
                        randomFloat(),
                        randomString(),
                        randomInt(),
                        randomInt(from = 1)
                    )
                )
            }
            return list
        }

        private fun getMockAddresses() = listOf(
            Address(Locale.getDefault()).apply { setAddressLine(1, "One") },
            Address(Locale.getDefault()).apply { setAddressLine(1, "Two") },
            Address(Locale.getDefault()).apply { setAddressLine(1, "Three") }
        )

        private fun randomLatLng() = LatLng(randomLatitude(), randomLongitude())

        private fun randomLatitude() = randomDouble(-90.0, 90.0)
        private fun randomLongitude() = randomDouble(-180.0, 180.0)
    }

    private val dataRepositoryMock = mock<DataRepository>()
    private val notificationRepositoryMock = mock<NotificationRepository>()
    private val mockGeofencingClient = mock<GeofencingClient>()

    override val sut by lazy {
        GeofenceRepositoryImpl(
            contextMock,
            notificationRepositoryMock,
            dataRepositoryMock,
            scope
        )
    }

    override fun setup() {
        super.setup()
        mockkStatic(LocationServices::class)
        every { LocationServices.getGeofencingClient(any<Context>()) } answers {
            mockGeofencingClient
        }
    }

    override fun Context.context() {
        every {
            checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
    }

    @Test
    fun testUpdateLastLocation() = runTest {
        assertTrue(sut.lastLocation == null)
        val mock = randomLatLng()
        sut.updateLastLocation(mock)
        assertTrue(sut.lastLocation == mock)
    }

    @Test
    fun testRegisterGeofencesNoPermission() = runTest {
        val fences = getMockGeofences()
        sut.registerGeofences(fences)
        verify {
            notificationRepositoryMock.showNotification(
                NotificationId.BACKGROUND_LOCATION,
                NotificationChannel.ERROR,
                any()
            )
        }
    }

    @Test
    fun testRegisterGeofences() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        val fences = getMockGeofences()
        val requestCapture = slot<GeofencingRequest>()
        every { mockGeofencingClient.addGeofences(capture(requestCapture), any()) } returns mock()
        sut.registerGeofences(fences)
        verify(exactly = 1) {
            mockGeofencingClient.addGeofences(any(), any())
        }
        fences.forEach { requirement ->
            val geofence = requestCapture.captured.geofences.firstOrNull {
                it.latitude == requirement.latitude && it.longitude == requirement.longitude
            } ?: throw AssertionError("Could not find matching geofence")
            assertTrue(geofence.loiteringDelay == requirement.loiteringDelay)
            assertTrue(geofence.notificationResponsiveness == requirement.notificationResponsiveness)
            assertTrue(geofence.radius == requirement.radius)
        }
    }

    @Test
    fun testGeocodeLocation() = runTest {
        val mockAddresses = getMockAddresses()
        mockkConstructor(Geocoder::class)
        every {
            anyConstructed<Geocoder>().getFromLocation(any(), any(), any(), any())
        } answers {
            arg<Geocoder.GeocodeListener>(3).onGeocode(mockAddresses)
        }
        val actual = sut.geocodeLocation(randomLatLng())
        assertTrue(actual == mockAddresses.first())
    }

    @Test
    fun testGeocodeLocationFail() = runTest {
        mockkConstructor(Geocoder::class)
        every {
            anyConstructed<Geocoder>().getFromLocation(any(), any(), any(), any())
        } answers {
            arg<Geocoder.GeocodeListener>(3).onError(randomString())
        }
        val actual = sut.geocodeLocation(randomLatLng())
        assertTrue(actual == null)
    }

    @Test
    fun testHasLocationPermissionGranted() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        assertTrue(sut.hasLocationPermission())
    }

    @Test
    fun testHasLocationPermissionDenied() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        assertFalse(sut.hasLocationPermission())
    }

    @Test
    fun testHasBackgroundLocationPermissionGranted() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        assertTrue(sut.hasBackgroundLocationPermission())
    }

    @Test
    fun testHasBackgroundLocationPermissionDenied() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        assertFalse(sut.hasBackgroundLocationPermission())
    }

    @Test
    fun testGeofenceLimitNotReached() = runTest {
        every {
            dataRepositoryMock.getRequirementData(
                RequirementDataType.GEOFENCE, GeofenceRequirementData::class.java
            )
        } returns flowOf(getMockGeofences())
        assertFalse(sut.isGeofenceLimitReached())
    }

    @Test
    fun testGeofenceLimitReached() = runTest {
        every {
            dataRepositoryMock.getRequirementData(
                RequirementDataType.GEOFENCE, GeofenceRequirementData::class.java
            )
        } returns flowOf(getMockGeofences(100))
        assertTrue(sut.isGeofenceLimitReached())
    }

}